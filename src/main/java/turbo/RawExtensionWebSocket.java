package turbo;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Registration;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.websocket.Direction;
import burp.api.montoya.websocket.extension.ExtensionWebSocket;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import burp.api.montoya.websocket.extension.ExtensionWebSocketMessageHandler;
import data.FrameData;
import data.FrameType;
import data.RawWebsocketConfig;
import logger.Logger;
import logger.LoggerLevel;
import utils.Utilities;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.Utilities.*;

public class RawExtensionWebSocket implements ExtensionWebSocket {
    private static final String[] ENABLED_TLS_PROTOCOLS = new String[]{"TLSv1.3", "TLSv1.2"};
    private static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 8192;
    private static final long DEFAULT_TICK = 10;
    private final Logger logger;
    private final RawWebsocketConfig config;
    private final String host;
    private final int port;
    private final boolean isSecure;
    private final RawWebSocketImpl engine;
    //    private final ExecutorService executorService;
    private final TurboThreadExecutor executorService;
    private final SocketFactory socketFactory = null;
    public int engineIOPingTimeout = 0;
    private final int connectionTimeout;
    private Socket socket;
    private OutputStream ostream;
    private InputStream istream;
    private WebSocketReadThread readThread;
    private WebsocketWriteThread writeThread;
    private ExtensionWebSocketCreationStatus status;
    private HttpResponse response;
    private int parallel = 0;
    private long latency = 0;
    private long watchDog = System.currentTimeMillis();
    private long enginIOTick = System.currentTimeMillis();

    private RawWebSocketExtensionWebSocketMessageHandler messageHandler;

    public RawExtensionWebSocket(String host, int port, boolean isSecure, byte[] handshake, Logger logger, RawWebsocketConfig config) throws RawException {
        this.host = host;
        this.port = port;
        this.isSecure = isSecure;
        this.logger = logger;
        this.config = config;
        this.engine = new RawWebSocketImpl(this);
        this.executorService = new TurboThreadExecutor(logger, 2);
        this.connectionTimeout = config.timeout() == 0 ? 3 : config.timeout();
//        this.executorService = Executors.newFixedThreadPool(2);

        try {
            initializeSocket();
            performHandshake(handshake);
            if (config.isPinging()) {
                ping();
                tune();
            }

            startWriteThread();
            startReadThread();
        } catch (Exception e) {
            this.status = ExtensionWebSocketCreationStatus.INVALID_REQUEST;
            throw new RawException(e.getMessage());
        }
    }

    public boolean isOpen() {
        return status == ExtensionWebSocketCreationStatus.STREAMING_RESPONSE;
    }

    private boolean prepareSocket() throws IOException {
        boolean upgradeSocketToSSLSocket = false;
        if (socketFactory != null) {
            socket = socketFactory.createSocket();
        } else if (socket == null) {
            socket = new Socket();
            upgradeSocketToSSLSocket = true;
        } else if (socket.isClosed()) {
            throw new IOException();
        }
        return upgradeSocketToSSLSocket;
    }

    private void upgradeSocketToSSL()
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLSocketFactory factory;
        if (socketFactory instanceof SSLSocketFactory) {
            factory = (SSLSocketFactory) socketFactory;
        } else {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
            factory = sslContext.getSocketFactory();
        }
        socket = factory.createSocket(socket, host, port, true);
    }

    private void initializeSocket() throws IOException {
        try {
            boolean upgradeSocketToSSLSocket = prepareSocket();
            socket.setTcpNoDelay(true);
            socket.setReuseAddress(true);
            if (!socket.isConnected()) {
                InetSocketAddress addr = new InetSocketAddress(host, port);
                socket.connect(addr, connectionTimeout * 1000);
            }
            if (isSecure) {
                upgradeSocketToSSL();
            }

            if (socket instanceof SSLSocket sslSocket) {
                sslSocket.setEnabledProtocols(ENABLED_TLS_PROTOCOLS);
            }

            istream = socket.getInputStream();
            ostream = socket.getOutputStream();
        } catch (Exception e) {
            logger.logOutput(LoggerLevel.UI, "Connection failed: " + e.getMessage());
            this.status = ExtensionWebSocketCreationStatus.CONNECTION_FAILED;
        }
    }

    private void performHandshake(byte[] handshake) throws IOException {
        ostream.write(handshake);
        ostream.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        String line;
        StringBuilder res = new StringBuilder();
        boolean upgradeSuccessful = false;

        while ((line = reader.readLine()) != null) {
            res.append(line);
            if (line.contains("HTTP/1.1 101")) {
                upgradeSuccessful = true;
            }
            if (line.isEmpty()) {
                break;
            }
        }

        if (upgradeSuccessful) {
            this.status = ExtensionWebSocketCreationStatus.STREAMING_RESPONSE;
        } else {
            logger.logOutput(LoggerLevel.UI, "NON UPGRADE RESPONSE: " + res);
            this.status = ExtensionWebSocketCreationStatus.NON_UPGRADE_RESPONSE;
        }
        this.response = HttpResponse.httpResponse(res.toString());
    }

    private void startWriteThread() {
        writeThread = new WebsocketWriteThread(logger);
        executorService.submitTask(writeThread);
    }

    private void startReadThread() {
        readThread = new WebSocketReadThread(logger, istream);
        executorService.submitTask(readThread);
    }

    private void ping() throws RawException {
        long ts = System.currentTimeMillis();
        boolean isTuning = true;
        byte[] buf = new byte[DEFAULT_READ_BUFFER_SIZE];
        int bytesRead;
        try {
            while (isTuning && (bytesRead = istream.read(buf)) != -1) {
                List<FrameData> frames = engine.decodeFrames(ByteBuffer.wrap(buf, 0, bytesRead));
                for (FrameData frame : frames) {
                    switch (frame.getOpcode()) {
                        case PING -> engine.outQueue.put(encodeFrame(FrameData.pongFrame(frame.getPayload())));
                        case TEXT -> analyse(new String(frame.getPayload(), Charset.defaultCharset()));
                        case CLOSE -> {
                            logger.logOutput(LoggerLevel.UI, "Connection was closed during initial setup!");
                            throw new RawException(decodeWebSocketCloseFrame(frame.getPayload()));
                        }
                    }
                }
                if (System.currentTimeMillis() - ts > connectionTimeout * 1000L) isTuning = false;
            }
        } catch (IOException | InterruptedException | RawException exception) {
            logger.logOutput(LoggerLevel.UI, "Connection was closed during initial setup!");
            closeConnection(exception.getMessage().getBytes(StandardCharsets.UTF_8));
            throw new RawException(exception.getMessage());
        }
    }

    private void analyse(String message) {
        logger.logOutput(LoggerLevel.UI, message);
        if (this.engineIOPingTimeout == 0) {
            try {
                EngineIO.EngineIoMessage em = EngineIO.parseMessage(message);
                this.engineIOPingTimeout = em.getPingInterval();
                logger.logOutput(LoggerLevel.UI, "Engine.IO handshake received from the server. Adjusting ping session: " + em.getPingInterval());
            } catch (IllegalArgumentException exception) {

            }
        } else {
            if (message.equalsIgnoreCase("40"))
                logger.logOutput(LoggerLevel.UI, "Socket.IO \"CONNECT\"");
        }
    }

    private void tune() throws RawException {
        try {
            long ts = System.currentTimeMillis();
            boolean isTuning = true;
            ByteBuffer magic = ByteBuffer.allocate(Integer.BYTES);
            magic.putInt(MAGIC);
            int[] steps = {128, 64, 32, 16, 8, 4, 2, 1};

            for (int step : steps) {
                byte[] tsBuffer = magicTime(ts, step);
                byte[] tuner = encodeFrame(FrameData.pingFrame(tsBuffer)).array();
                byte[] bulkTuner = new byte[tuner.length * step];

                // Copy tuners into bulk array
                for (int i = 0; i < step; i++) {
                    System.arraycopy(tuner, 0, bulkTuner, i * tuner.length, tuner.length);
                }

                // Send all tuners in one bulk write
                ostream.write(bulkTuner);
                ostream.flush();
            }

            byte[] buf = new byte[DEFAULT_READ_BUFFER_SIZE];
            int bytesRead;
            while (isTuning && (bytesRead = istream.read(buf)) != -1) {
                List<FrameData> frames = engine.decodeFrames(ByteBuffer.wrap(buf, 0, bytesRead));
                for (FrameData frame : frames) {
                    switch (frame.getOpcode()) {
                        case PONG -> {
                            if (startsWith(frame.getPayload(), magic.array())) {
                                latency = System.currentTimeMillis() - ts;
                            }
                        }
                        case PING -> engine.outQueue.put(encodeFrame(FrameData.pongFrame(frame.getPayload())));
                        case TEXT -> {
                            logger.logOutput(LoggerLevel.UI, new String(frame.getPayload(), Charset.defaultCharset()));
                        }
                        case CLOSE -> {
                            logger.logOutput(LoggerLevel.UI, "Server closed connection during tuning! Check if ping frame is supported!");
                            throw new RawException(decodeWebSocketCloseFrame(frame.getPayload()));
                        }
                    }
                }
                Map<Integer, Long> frequencyMap = frames.stream()
                        .map(FrameData::getPayload)
                        .filter(payload -> payload.length == 16)
                        .map(Utilities::magicIndex)
                        .collect(Collectors.groupingBy(
                                number -> number,
                                Collectors.counting()));
                Map<Integer, Long> sortedMap = frequencyMap.entrySet().stream()
                        .sorted(Map.Entry.<Integer, Long>comparingByKey().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));
                for (Map.Entry<Integer, Long> entry : sortedMap.entrySet()) {
                    if (entry.getValue().intValue() == entry.getKey()) {
                        logger.logOutput(LoggerLevel.UI, "Engine tuned successfully. TCP Frames number is: " + entry.getKey());
                        parallel = entry.getKey();
                        isTuning = false;
                        break;
                    }
                }

                if (System.currentTimeMillis() - ts > connectionTimeout * 1000L) isTuning = false;
            }
        } catch (RawException | IOException | InterruptedException exception) {
            closeConnection(exception.getMessage().getBytes(StandardCharsets.UTF_8));
            throw new RawException(exception.getMessage());
        }
    }

    public void messageReceived(FrameData data) {
        int out = engine.outCounter.getAndIncrement();
        if (engineIOPingTimeout > 0 && data.getOpcode() == FrameType.TEXT) {
            String pong = new String(data.getPayload(), Charset.defaultCharset());
            if (pong.equalsIgnoreCase("3")) return;
        }
        String comment = String.format("Latency %d", latency);
        messageHandler.textMessageReceived(
                out,
                new RawTextMessage(data.getPayload(), Direction.SERVER_TO_CLIENT),
                comment,
                LocalDateTime.now());
    }

    public void closeConnection(byte[] reason) {
        logger.logOutput(LoggerLevel.UI, "Closing websocket: " + new String(reason));

        if (readThread != null) {
            readThread.stopReading();
        }

        if (writeThread != null) {
            writeThread.stopWriting();
        }
        executorService.shutdown();
    }

    public void updateLatency(long magicLatency) {
        latency = System.currentTimeMillis() - magicLatency;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public ExtensionWebSocketCreationStatus getStatus() {
        return status;
    }

    @Override
    public void sendTextMessage(String s) {
        try {
            engine.outQueue.put(encodeFrame(FrameData.textFrame(s)));
        } catch (InterruptedException e) {
            closeConnection(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void sendBinaryMessage(ByteArray byteArray) {
        try {
            engine.outQueue.put(encodeFrame(FrameData.binaryFrame(byteArray.getBytes())));
        } catch (InterruptedException e) {
            closeConnection(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void close() {
        closeConnection("Burp closed connection".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Registration registerMessageHandler(ExtensionWebSocketMessageHandler extensionWebSocketMessageHandler) {
        this.messageHandler = (RawWebSocketExtensionWebSocketMessageHandler) extensionWebSocketMessageHandler;
        return null;
    }

    private class WebSocketReadThread implements Runnable {
        private final Logger logger;
        private final InputStream inputStream;
        private volatile boolean running = true;

        WebSocketReadThread(Logger logger, InputStream inputStream) {
            this.logger = logger;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[DEFAULT_READ_BUFFER_SIZE];
            ByteBuffer accumulatedBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
            int bytesRead;
            try {
                while (running && (bytesRead = inputStream.read(buffer)) != -1) {
                    accumulatedBuffer.put(buffer, 0, bytesRead);
                    accumulatedBuffer.flip();
                    engine.decode(accumulatedBuffer);
                    accumulatedBuffer.compact();
                }
            } catch (Exception e) {
                logger.logOutput(LoggerLevel.UI, "RAW WebSocketReadThread failed unexpectedly with: " + e.getMessage());
            } finally {
                closeConnection("Finished.".getBytes(StandardCharsets.UTF_8));
            }
        }

        public void stopReading() {
            logger.logOutput(LoggerLevel.UI, "Signal the thread to stop.");
            running = false;
        }
    }

    private class WebsocketWriteThread implements Runnable {
        private final Logger logger;
        private volatile boolean running = true;

        WebsocketWriteThread(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("WebSocketWriteThread-" + Thread.currentThread().getId());
            try {
                runWriteData();
            } catch (IOException | RuntimeException e) {
                logger.logOutput(LoggerLevel.UI, "RAW WebsocketWriteThread failed unexpectedly with: " + e.getMessage());
            } finally {
                closeSocket();
            }
        }

        private void writeChunk(List<ByteBuffer> chunk) throws IOException {
            List<ByteBuffer> array = new ArrayList<>(chunk);
            array.add(encodeFrame(FrameData.pingFrame(magicTime(System.currentTimeMillis(), engine.inCounter.getAndIncrement()))));
            // Calculate the total size of the chunk
            int size = array.stream().mapToInt(ByteBuffer::remaining).sum();

            // Create a single buffer for the chunk
            ByteBuffer combinedBuffer = ByteBuffer.allocate(size);

            // Combine all buffers into the single buffer
            for (ByteBuffer buffer : array) {
                combinedBuffer.put(buffer.slice());
            }

            // Prepare the combined buffer for reading
            combinedBuffer.flip();

            // Write the combined buffer to the output stream
            ostream.write(combinedBuffer.array(), 0, combinedBuffer.limit());
            ostream.flush();
        }

        /**
         * Write the data into the outstream
         *
         * @throws IOException if write or flush did not work
         */
        private void runWriteData() throws IOException {
//            try {
//                while (running && !Thread.interrupted()) {
//                    ByteBuffer buffer = engine.outQueue.take();
//                    ostream.write(buffer.array(), 0, buffer.limit());
//                    ostream.flush();
//                }
//            } catch (InterruptedException e) {
//                for (ByteBuffer buffer : engine.outQueue) {
//                    ostream.write(buffer.array(), 0, buffer.limit());
//                    ostream.flush();
//                }
//                Thread.currentThread().interrupt();
//            }
            while (running && !Thread.interrupted()) {
                long now = System.currentTimeMillis();
                long tick = Math.max(DEFAULT_TICK, latency);

                if (engineIOPingTimeout > 0 && now - enginIOTick > engineIOPingTimeout) {
                    engine.inCounter.getAndIncrement();
                    ostream.write(encodeFrame(FrameData.textFrame("2")).array());
                    enginIOTick = now;
                }

                if (now - watchDog > tick) {
                    List<ByteBuffer> bufferList = new ArrayList<>();
                    int totalSize = 0;

                    // Take all ByteBuffers from the queue
                    engine.outQueue.drainTo(bufferList);

                    if (bufferList.isEmpty()) {
                        continue;
                    }

                    if (parallel > 1) {
                        List<List<ByteBuffer>> chunks = IntStream.range(0, (bufferList.size() + parallel - 1) / parallel)
                                .mapToObj(i -> bufferList.subList(i * parallel, Math.min((i + 1) * parallel, bufferList.size())))
                                .toList();
                        for (List<ByteBuffer> chunk : chunks) {
                            writeChunk(chunk);
                        }
                    } else {

                        // Calculate the total size of the data
                        for (ByteBuffer buffer : bufferList) {
                            totalSize += buffer.remaining();
                        }

                        // Process buffers
                        int currentChunkSize = 0;
                        List<ByteBuffer> chunk = new ArrayList<>();

                        for (ByteBuffer buffer : bufferList) {
                            int bufferSize = buffer.remaining();

                            // Check if adding this buffer would exceed the chunk size limit
                            if (currentChunkSize + bufferSize > DEFAULT_WRITE_BUFFER_SIZE) {
                                // Write the current chunk to the output stream
                                writeChunk(chunk);
                                chunk.clear();
                                currentChunkSize = 0;
                            }

                            // Add the buffer to the current chunk
                            chunk.add(buffer);
                            currentChunkSize += bufferSize;
                        }

                        // Write the last chunk if there are remaining buffers
                        if (!chunk.isEmpty()) {
                            writeChunk(chunk);
                        }
                    }
                    watchDog = now;
                }
            }
        }

        private void bye() {
            try {
                engine.outQueue.put(encodeFrame(FrameData.closeFrame()));
            } catch (InterruptedException ignored) {
            }
        }

        public void stopWriting() {
            bye();
            running = false; // Signal the thread to stop
        }

        /**
         * Closing the socket
         */
        private void closeSocket() {
            try {
                if (istream != null) {
                    istream.close();
                }
                if (ostream != null) {
                    ostream.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // TODO
            }
        }
    }
}
