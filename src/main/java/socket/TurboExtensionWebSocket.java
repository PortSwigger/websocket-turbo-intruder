package socket;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Registration;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.websocket.Direction;
import burp.api.montoya.websocket.extension.ExtensionWebSocket;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import burp.api.montoya.websocket.extension.ExtensionWebSocketMessageHandler;
import data.FrameData;
import logger.Logger;
import logger.LoggerLevel;
import standalone.TurboHttpResponse;
import turbo.TurboRuntimeException;
import turbo.TurboThreadExecutor;
import utils.Utilities;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;
import static utils.Utilities.INITIAL_MESSAGE;
import static utils.Utilities.encodeFrame;

public class TurboExtensionWebSocket implements ExtensionWebSocket {
    private static final String[] ENABLED_TLS_PROTOCOLS = new String[]{"TLSv1.3", "TLSv1.2"};
    private static final int DEFAULT_QUEUE_SIZE = 1024;
    private static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 8192;
    public final AtomicInteger reconnectCounter = new AtomicInteger(0);
    private final String host;
    private final int port;
    private final boolean isSecure;
    private final byte[] handshake;
    private final TurboEngine engine;
    private final TurboThreadExecutor executorService;
    private final Logger logger;
    private final TurboConfig config;
    private final BlockingQueue<FrameData> queue;
    private final Lock reconnectLock = new ReentrantLock();
    // Socket and streams
    private Socket socket;
    private OutputStream ostream;
    private InputStream istream;

    // Threads
    private volatile TurboReadThread readThread;
    private volatile TurboWriteThread writeThread;

    private ExtensionWebSocketCreationStatus status;
    private TurboHttpResponse response;
    private TurboWebSocketExtensionWebSocketMessageHandler messageHandler;

    public TurboExtensionWebSocket(String host, int port, boolean isSecure, byte[] handshake, Logger logger, TurboConfig config)
            throws TurboRuntimeException {
        this.host = host;
        this.port = port;
        this.isSecure = isSecure;
        this.handshake = handshake;
        this.logger = logger;
        this.config = config;
        this.engine = new TurboEngine(this, logger);
        this.executorService = new TurboThreadExecutor(logger, 2);
        this.queue = new LinkedBlockingQueue<>();

        try {
            // 1) Create initial socket + handshake
            initializeSocket(false);
            performHandshake();

            // 2) Start read/write threads
            startReadThread();
            startWriteThread();

            // 3) Handle incoming frames
            setup();

        } catch (IllegalArgumentException exception) {
            this.status = ExtensionWebSocketCreationStatus.INVALID_HOST;
            throw new TurboRuntimeException(exception.getMessage());
        } catch (IOException exception) {
            this.status = ExtensionWebSocketCreationStatus.CONNECTION_FAILED;
            throw new TurboRuntimeException(exception.getMessage());
        }
    }

    private void initializeSocket(boolean reconnect) throws IOException, IllegalArgumentException {
        // If not reconnecting, or if socket is null/closed, create a fresh socket
        if (socket == null || reconnect || socket.isClosed()) {
            socket = new Socket();
        }

        configureSocket();

        if (isSecure) {
            enableSSL();
        }

        if (socket instanceof SSLSocket sslSocket) {
            sslSocket.setEnabledProtocols(ENABLED_TLS_PROTOCOLS);
        }

        istream = socket.getInputStream();
        ostream = socket.getOutputStream();
    }

    private void configureSocket() throws IOException, IllegalArgumentException {
        socket.setTcpNoDelay(true);
        socket.setReuseAddress(true);
        if (!socket.isConnected()) {
            InetSocketAddress address = new InetSocketAddress(host, port);
            socket.connect(address, config.connectionTimeout() * 1000);
        }
    }

    private void enableSSL() throws IOException {
        TrustManager[] trustManagers = new TrustManager[]{
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

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            socket = sslSocketFactory.createSocket(socket, host, port, true);
        } catch (Exception e) {
            throw new IOException("SSL init failed: " + e.getMessage(), e);
        }
    }

    private void performHandshake() throws IOException {
        ostream.write(handshake);
        ostream.flush();

        BufferedInputStream bufferedStream = new BufferedInputStream(istream);
        StringBuilder sbResponse = new StringBuilder();

        boolean upgradeSuccessful = false;
        byte[] buffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = bufferedStream.read(buffer)) != -1) {
            String chunk = new String(buffer, 0, bytesRead);
            sbResponse.append(chunk);

            if (chunk.startsWith("HTTP/1.1 101")) {
                upgradeSuccessful = true;
            }
            if (sbResponse.toString().contains("\r\n\r\n")) {
                // Headers done
                break;
            }
        }

        if (upgradeSuccessful) {
            this.status = ExtensionWebSocketCreationStatus.SUCCESS;
        } else {
            logger.logOutput(LoggerLevel.UI, "NON UPGRADE RESPONSE:\n" + sbResponse);
            this.status = ExtensionWebSocketCreationStatus.NON_UPGRADE_RESPONSE;
        }
        this.response = new TurboHttpResponse(sbResponse.toString());
    }

    private void setup() {
        // Optional setup code before the actual WebSocket usage
        try {
            sleep(config.sendingInterval());
            if (config.helloMessage() != null) {
                engine.outQueue.put(Utilities.encodeFrame(FrameData.textFrame(config.helloMessage())));
            }
            if (config.isPinging()) {
                long ts = System.currentTimeMillis();
                engine.outQueue.put(Utilities.encodeFrame(FrameData.pingFrame(Utilities.magicPing(ts))));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Safely starts the read thread.
     */
    private void startReadThread() {
        readThread = new TurboReadThread(logger);
        executorService.submitTask(readThread);
    }

    /**
     * Safely starts the write thread.
     */
    private void startWriteThread() {
        writeThread = new TurboWriteThread(logger);
        executorService.submitTask(writeThread);
    }

    /**
     * Stop both read and write threads if running.
     */
    private void stopThreads() {
        if (readThread != null) {
            readThread.stopReading();
            readThread = null;
        }
        if (writeThread != null) {
            writeThread.stopWriting();
            writeThread = null;
        }
    }

    /**
     * Closes and nullifies the socket and streams.
     */
    public void closeSocket() {
        try {
            if (istream != null) {
                istream.close();
            }
        } catch (IOException ignore) {
        }
        try {
            if (ostream != null) {
                ostream.close();
            }
        } catch (IOException ignore) {
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignore) {
        }

        socket = null;
        istream = null;
        ostream = null;
    }

    /**
     * Attempt to reconnect if not exceeding max attempts.
     * If successful, start new read/write threads.
     */
    public void reconnect() {
        // We only want one reconnect at a time
        if (reconnectLock.tryLock()) {
            try {
                // If we are already at max attempts, give up
                if (reconnectCounter.getAndIncrement() >= config.retries()) {
                    logger.logOutput(LoggerLevel.DEBUG, "Max reconnect attempts reached. Closing.");
                    closeConnection();
                    return;
                }

                // Stop current threads and close old socket
                stopThreads();
                closeSocket();

                boolean success = false;
                // Attempt actual reconnect
                try {
                    initializeSocket(true);
                    performHandshake();

                    if (isOpen()) {
                        logger.logOutput(LoggerLevel.DEBUG, "Reconnected successfully.");
                        success = true;
                        // Restart threads with new socket/streams
                        startReadThread();
                        startWriteThread();
                    }
                } catch (SocketTimeoutException timeout) {
                    logger.logOutput(LoggerLevel.DEBUG, "Aborting! Reconnect attempt timeout: " + timeout.getMessage());
                    reconnectCounter.set(Integer.MAX_VALUE);
                } catch (IOException ioEx) {
                    logger.logOutput(LoggerLevel.DEBUG, "Reconnect attempt failed: " + ioEx.getMessage());
                }

                if (!success) {
                    logger.logOutput(LoggerLevel.DEBUG, "Reconnect failed, attempt #" + reconnectCounter.get());

                    // Check if we still have attempts left, otherwise close
                    if (reconnectCounter.get() >= config.retries()) {
                        logger.logOutput(LoggerLevel.DEBUG, "Failed to reconnect after max attempts.");
                        closeConnection();
                    }
                }

            } finally {
                reconnectLock.unlock();
            }
        } else {
            logger.logOutput(LoggerLevel.DEBUG, "Reconnect already in progress or lock timeout. Skipping.");
        }
    }

    public void textMessageReceived(int id, byte[] payload, String comment) {
        messageHandler.textMessageReceived(
                id,
                new TurboTextMessage(payload, Direction.SERVER_TO_CLIENT),
                comment,
                LocalDateTime.now());
    }

    /**
     * Called when a valid (decoded) FrameData is received from the server.
     */
    public void messageReceived(FrameData data) {
        try {
            if (messageHandler != null) {
                // Drain anything previously in the queue
                List<FrameData> unprocessed = new ArrayList<>();
                queue.drainTo(unprocessed);
                for (FrameData d : unprocessed) {
                    textMessageReceived(-1, d.getPayload(), INITIAL_MESSAGE);
                }
                textMessageReceived(engine.outCounter.getAndIncrement(), data.getPayload(), "");
            } else {
                queue.put(data);
            }
        } catch (InterruptedException e) {
            logger.logOutput(LoggerLevel.UI,
                    "Unprocessed message received " + (new String(data.getPayload()) + e.getMessage()));
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cleanly closes the WebSocket connection, shutting down threads and the socket.
     */
    public void closeConnection() {
        stopThreads();
        closeSocket();
        this.status = ExtensionWebSocketCreationStatus.CONNECTION_FAILED;
        executorService.shutdown();
    }

    public HttpResponse getResponse() {
        return response;
    }

    public ExtensionWebSocketCreationStatus getStatus() {
        return status;
    }

    public void sendByteBuffer(ByteBuffer buffer) {
        try {
            engine.outQueue.put(encodeFrame(buffer));
        } catch (IllegalArgumentException e) {
            logger.logOutput(LoggerLevel.DEBUG, "Broken frame data" + e.getMessage());
        } catch (InterruptedException e) {
            closeConnection();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Burp's ExtensionWebSocket interface methods below
     */
    @Override
    public void sendTextMessage(String s) {
        try {
            engine.outQueue.put(encodeFrame(FrameData.textFrame(s)));
        } catch (IllegalArgumentException e) {
            logger.logOutput(LoggerLevel.DEBUG, "Broken frame data" + e.getMessage());
        } catch (InterruptedException e) {
            closeConnection();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void sendBinaryMessage(ByteArray byteArray) {
    }

    @Override
    public void close() {
        try {
            logger.logOutput(LoggerLevel.UI, "User closed connection");
            reconnectCounter.set(Integer.MAX_VALUE);

            engine.outQueue.put(encodeFrame(FrameData.closeFrame()));
            sleep(config.sendingInterval());
            queue.clear();
            engine.inQueue.clear();
            engine.outQueue.clear();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            closeConnection();
        }
    }

    @Override
    public Registration registerMessageHandler(ExtensionWebSocketMessageHandler extensionWebSocketMessageHandler) {
        this.messageHandler = (TurboWebSocketExtensionWebSocketMessageHandler) extensionWebSocketMessageHandler;
        return null;
    }

    /**
     * Check if the socket is open, i.e. connected and not closed.
     */
    public boolean isOpen() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

//    public void registerBarrier(CyclicBarrier barrier) {
//        this.barrier = barrier;
//    }

    // ----------------------------------------------
    //               Inner Classes
    // ----------------------------------------------

    private class TurboReadThread implements Runnable {
        private final Logger logger;
        private volatile boolean running = true;

        TurboReadThread(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[DEFAULT_READ_BUFFER_SIZE];
            ByteBuffer accumulatedBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    int bytesRead = istream.read(buffer);
                    if (bytesRead == -1) {
                        // Server closed connection
                        if (running) {
                            logger.logOutput(LoggerLevel.DEBUG, "Socket closed by server. Attempting reconnect...");
                            stopReading(); // stop this thread
                            reconnect();
                        }
                        break;
                    }

                    // Ensure there's enough space in accumulatedBuffer
                    if (accumulatedBuffer.remaining() < bytesRead) {
                        ByteBuffer newBuffer = ByteBuffer.allocate(accumulatedBuffer.capacity() + DEFAULT_READ_BUFFER_SIZE);
                        accumulatedBuffer.flip();
                        newBuffer.put(accumulatedBuffer);
                        accumulatedBuffer = newBuffer;
                    }

                    accumulatedBuffer.put(buffer, 0, bytesRead);
                    accumulatedBuffer.flip();

                    // Decode frames
                    engine.decode(accumulatedBuffer);

                    accumulatedBuffer.compact();
                    sleep(config.sendingInterval());
                } catch (IllegalArgumentException e) {
                    if (running) {
                        logger.logOutput(LoggerLevel.DEBUG, e.getMessage());

                        accumulatedBuffer.flip();
                        byte[] validBytes = new byte[accumulatedBuffer.remaining()];
                        accumulatedBuffer.get(validBytes);

                        logger.logOutput(LoggerLevel.DEBUG, ByteBuffer.wrap(validBytes));
                        stopReading();
                        reconnect();
                    }
                    break;
                } catch (Exception e) {
                    if (running) {
                        logger.logOutput(LoggerLevel.DEBUG, "General error in read thread: " + e.getMessage());
                        stopReading();
                        reconnect();
                    }
                    break;
                }
            }
        }

        public void stopReading() {
            running = false;
        }
    }

    private class TurboWriteThread implements Runnable {
        private final Logger logger;
        private volatile boolean running = true;

        TurboWriteThread(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void run() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    writeQueuedData();
                    sleep(config.sendingInterval() <= 0 ? 10 : config.sendingInterval());
                } catch (IOException ex) {
                    if (running) {
                        logger.logOutput(LoggerLevel.DEBUG, "IOException in write thread: " + ex.getMessage());
                        stopWriting();
                        reconnect();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (running) {
                        logger.logOutput(LoggerLevel.DEBUG, "General error in write thread: " + e.getMessage());
                        stopWriting();
                        reconnect();
                    }
                    break;
                }
            }
        }

        private void writeQueuedData() throws IOException, InterruptedException {
            if (config.useChunks()) {
                List<ByteBuffer> bufferList = new ArrayList<>();
                // Drain up to DEFAULT_QUEUE_SIZE frames
                for (int i = 0; i < DEFAULT_QUEUE_SIZE; i++) {
                    ByteBuffer buffer = engine.outQueue.poll();
                    if (buffer == null) {
                        break;
                    }
                    bufferList.add(buffer);
                }

                if (bufferList.isEmpty()) {
                    return;
                }

                // We chunk the data to avoid writing huge bursts if large queue
                int currentChunkSize = 0;
                List<ByteBuffer> chunk = new ArrayList<>();

                for (ByteBuffer buf : bufferList) {
                    int bufferSize = buf.remaining();
                    if (currentChunkSize + bufferSize > DEFAULT_WRITE_BUFFER_SIZE) {
                        // flush current chunk
                        writeChunk(chunk);
                        chunk.clear();
                        currentChunkSize = 0;
                    }
                    chunk.add(buf);
                    currentChunkSize += bufferSize;
                }
                // flush the last chunk
                if (!chunk.isEmpty()) {
                    writeChunk(chunk);
                }
            } else {
                ByteBuffer buffer = engine.outQueue.take();
                writeChunk(List.of(buffer));
            }
        }

        private void writeChunk(List<ByteBuffer> chunk) throws IOException {
            // Calculate total size
            int size = 0;
            for (ByteBuffer b : chunk) {
                size += b.remaining();
            }
            ByteBuffer combined = ByteBuffer.allocate(size);
            for (ByteBuffer b : chunk) {
                combined.put(b.slice());
            }
            combined.flip();
            ostream.write(combined.array(), 0, combined.limit());
            ostream.flush();
        }

        public void stopWriting() {
            running = false;
        }
    }
}
