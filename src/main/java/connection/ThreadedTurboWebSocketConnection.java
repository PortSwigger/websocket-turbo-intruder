package connection;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.Direction;
import burp.api.montoya.websocket.extension.ExtensionWebSocket;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import data.AttackIdAndWebSocketConnectionMessage;
import data.ConnectionMessage;
import data.PendingMessages;
import logger.Logger;
import logger.LoggerLevel;
import socket.*;
import turbo.TurboThreadExecutor;
import utils.PayloadManipulator;
import utils.Utilities;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;
import static java.lang.Thread.sleep;
import static java.time.LocalDateTime.now;

public class ThreadedTurboWebSocketConnection implements Connection, TurboWebSocketInterface {
    private static final int DEFAULT_INTERVAL = 10;
    public final BlockingQueue<ConnectionMessage> outQueue;
    //    public final AtomicInteger outCounter = new AtomicInteger(0);
    private final Logger logger;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final TurboWebSockets webSockets;
    private final HttpRequest upgradeRequest;
    private final TurboConfig config;
    private final PayloadManipulator payloadManipulator;
    private final List<TurboExtensionWebSocket> webSocketPool = new CopyOnWriteArrayList<>();
    private final TurboThreadExecutor senderExecutor;
    // For simple round-robin, keep a counter
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final AtomicInteger outCounter = new AtomicInteger(0);
    private final CounterMap counterMap;
    private boolean running;

    public ThreadedTurboWebSocketConnection(
            Logger logger,
            PendingMessages pendingMessages,
            int attackId,
            HttpRequest upgradeRequest,
            TurboWebSocketMessagePublisher publisher,
            TurboConfig config
    ) throws Exception {
        this.logger = logger;
        this.webSockets = new TurboWebSockets(logger, config);
        this.pendingMessages = pendingMessages;
        this.attackId = attackId;
        this.upgradeRequest = upgradeRequest;
        this.config = config;
        this.payloadManipulator = new PayloadManipulator();
        this.outQueue = new LinkedBlockingQueue<>();
        this.counterMap = new CounterMap(config.nThreads());
        this.senderExecutor = new TurboThreadExecutor(logger, config.nThreads());

        publisher.addSubscriber(this);
        createMultipleConnections(config.nThreads());
        startMessageDispatchers(config.nThreads());
    }

    private void createMultipleConnections(int poolSize) {
        for (int i = 0; i < poolSize; i++) {
            try {
                TurboExtensionWebSocket socket = createExtensionWebSocket(upgradeRequest, i + 1);
                webSocketPool.add(socket);
                logger.logOutput(LoggerLevel.DEBUG, "WebSocket #" + (i + 1) + " created successfully.");
            } catch (Exception e) {
                logger.logOutput(LoggerLevel.DEBUG, "Failed to create WebSocket #" + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    private void startMessageDispatchers(int numberOfThreads) {
        this.running = true;
        for (int i = 0; i < numberOfThreads; i++) {
            senderExecutor.submitTask(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        // If no websockets available, log or handle accordingly
                        if (webSocketPool.isEmpty()) {
                            continue;
                        }

                        // Round-robin index for picking a WebSocket
                        int index = roundRobinIndex.getAndUpdate(
                                val -> (val + 1) % webSocketPool.size()
                        );

                        TurboExtensionWebSocket socket = webSocketPool.get(index);

                        // Take the next message to send
                        ConnectionMessage nextMsg = outQueue.poll();
                        if (nextMsg == null) continue;
                        nextMsg.setHash(Utilities.generateID(index + 1, counterMap.getAndIncrement(index)));

//                        ByteArray arr = ByteArray.byteArray(nextMsg.getCode())
//                                .withAppended(ByteBuffer.allocate(Long.BYTES).putLong(nextMsg.getPayloadLength()).array())
//                                .withAppended(nextMsg.getMessageByteBuffer().array());
                        ByteBuffer arr = ByteBuffer.allocate(1 + Long.BYTES + nextMsg.getMessageByteBuffer().remaining())
                                .put((byte) nextMsg.getCode())
                                .putLong(nextMsg.getPayloadLength())
                                .put(nextMsg.getMessageByteBuffer().duplicate());
                        arr.flip();
                        // Attempt to send
                        sendInternal(socket, arr);
//                        int interval = config.sendingInterval() <= 0 ? 10 : config.sendingInterval() / webSocketPool.size();
                        sleep(DEFAULT_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.logOutput(LoggerLevel.DEBUG, "Error dispatching message: " + e.getMessage());
                        break;
                    }
                }
            });
        }
    }

    private void sendInternal(TurboExtensionWebSocket socket, String payload) {
        socket.sendTextMessage(payload);
    }

    private void sendInternal(TurboExtensionWebSocket socket, ByteBuffer payload) {
        socket.sendByteBuffer(payload);
    }

    @Override
    public void queue(String payload) {
        queue(outCounter.getAndIncrement(), payload, CLIENT_TO_SERVER);
    }

    @Override
    public void queue(int code, byte[] payload) {
        queue(code, payload, (long) payload.length);
    }

    @Override
    public void queue(int code, byte[] payload, int length) {
        queue(code, payload, (long) length);
    }

    @Override
    public void queue(int code, byte[] payload, String length) {
        queue(code, payload, Long.parseLong(length));
    }

    private void queue(int id, String payload, Direction direction) {
        queueInternal(new TurboWebSocketConnectionMessage(id, payload, direction, this));
    }

    private void queue(int code, byte[] payload, Long length) {
        queueInternal(new TurboWebSocketConnectionMessage(
                outCounter.getAndIncrement(), code, ByteBuffer.wrap(payload), CLIENT_TO_SERVER, null,
                now(), this, length));
    }

    private void queueInternal(TurboWebSocketConnectionMessage message) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, message));
    }


    @Override
    public void queue(String payload, String replacement) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new TurboWebSocketConnectionMessage(outCounter.getAndIncrement(), newPayload, CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queueWithComment(String payload, String comment) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new TurboWebSocketConnectionMessage(outCounter.getAndIncrement(), payload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void queueWithComment(String payload, String replacement, String comment) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new TurboWebSocketConnectionMessage(outCounter.getAndIncrement(), newPayload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void sendMessage(ConnectionMessage message) {
        outQueue.add(message);
    }

    @Override
    public HttpRequest upgradeRequest() {
        return upgradeRequest;
    }

    private TurboExtensionWebSocket createExtensionWebSocket(HttpRequest upgradeRequest, int id) throws Exception {
        TurboExtensionWebSocketCreation creation = webSockets.createWebSocket(upgradeRequest);

        Optional<ExtensionWebSocket> optionalWS = creation.webSocket();
        if (optionalWS.isEmpty()) {
            throw new Exception("Failed to create websocket connection");
        }

        TurboExtensionWebSocket extWS = (TurboExtensionWebSocket) optionalWS.get();
        extWS.registerMessageHandler(
                new TurboWebSocketExtensionWebSocketMessageHandler(
                        logger,
                        pendingMessages,
                        attackId,
                        id,
                        this
                )
        );
        return extWS;
    }

    @Override
    public boolean isOpen() {
        return !(webSocketPool.isEmpty());
    }

    @Override
    public ExtensionWebSocketCreationStatus status() {
        return ExtensionWebSocketCreationStatus.SUCCESS;
    }

    public void stop() {
        this.running = false;
        // Close websockets
        for (TurboExtensionWebSocket socket : webSocketPool) {
            socket.close();
        }
        webSocketPool.clear();
        outQueue.clear();
        counterMap.resetAll();

        // Shut down executor
        senderExecutor.shutdown();
    }
}
