package connection;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.extension.ExtensionWebSocket;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import data.AttackIdAndWebSocketConnectionMessage;
import data.ConnectionMessage;
import data.FrameType;
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

public class ThreadedTurboWebSocketConnection implements Connection {
    private static final int DEFAULT_INTERVAL = 10;
    public final BlockingQueue<ConnectionMessage> outQueue;
    private final AtomicInteger inCount = new AtomicInteger(0);
    private final AtomicInteger outCount = new AtomicInteger(0);
    private final Logger logger;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final TurboWebSockets webSockets;
    private final HttpRequest upgradeRequest;
    private final TurboConfig config;
    private final PayloadManipulator payloadManipulator;
    private final List<TurboExtensionWebSocket> webSocketPool = new CopyOnWriteArrayList<>();
    private final TurboThreadExecutor senderExecutor;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final CounterMap counterMap;
    private boolean running;

    public ThreadedTurboWebSocketConnection(
            Logger logger,
            PendingMessages pendingMessages,
            int attackId,
            HttpRequest upgradeRequest,
            TurboConnectionPublisher publisher,
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

    @Override
    public int queue(String payload) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(
                new AttackIdAndWebSocketConnectionMessage(attackId,
                        new TurboWebSocketConnectionMessage(
                                i,
                                payload,
                                CLIENT_TO_SERVER,
                                this)));
        return i;
    }

    @Override
    public int queue(byte[] payload) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(
                new AttackIdAndWebSocketConnectionMessage(attackId,
                        new TurboWebSocketConnectionMessage(
                                i,
                                FrameType.TEXT,
                                ByteBuffer.wrap(payload),
                                CLIENT_TO_SERVER,
                                null,
                                now(),
                                this,
                                payload.length)));
        return i;
    }

    @Override
    public int queue(FrameType code, byte[] payload) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(
                new AttackIdAndWebSocketConnectionMessage(attackId,
                        new TurboWebSocketConnectionMessage(
                                i,
                                code,
                                ByteBuffer.wrap(payload),
                                CLIENT_TO_SERVER,
                                null,
                                now(),
                                this,
                                payload.length)));
        return i;
    }

    @Override
    public int queue(FrameType code, byte[] payload, int length) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(
                new AttackIdAndWebSocketConnectionMessage(attackId,
                        new TurboWebSocketConnectionMessage(
                                i,
                                code,
                                ByteBuffer.wrap(payload),
                                CLIENT_TO_SERVER,
                                null,
                                now(),
                                this,
                                length)));
        return i;
    }

    @Override
    public int queue(FrameType code, byte[] payload, String length) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(
                new AttackIdAndWebSocketConnectionMessage(attackId,
                        new TurboWebSocketConnectionMessage(
                                i,
                                code,
                                ByteBuffer.wrap(payload),
                                CLIENT_TO_SERVER,
                                null,
                                now(),
                                this,
                                Long.parseLong(length))));
        return i;
    }

    @Override
    public int queue(String payload, String replacement) {
        int i = outCount.getAndIncrement();
        String newPayload = payloadManipulator.modify(payload, replacement);
        pendingMessages.accept(
                new AttackIdAndWebSocketConnectionMessage(attackId,
                        new TurboWebSocketConnectionMessage(
                                i,
                                newPayload,
                                CLIENT_TO_SERVER,
                                this)));
        return i;
    }

    @Override
    public void queueWithComment(String payload, String comment) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new TurboWebSocketConnectionMessage(outCount.getAndIncrement(), payload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void queueWithComment(String payload, String replacement, String comment) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new TurboWebSocketConnectionMessage(outCount.getAndIncrement(), newPayload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void sendMessage(ConnectionMessage message) {
        outQueue.add(message);
    }

    @Override
    public HttpRequest upgradeRequest() {
        return upgradeRequest;
    }

    @Override
    public ExtensionWebSocketCreationStatus status() {
        boolean success = webSocketPool.stream().allMatch(
                c -> c.getStatus() == ExtensionWebSocketCreationStatus.SUCCESS);
        return success ? ExtensionWebSocketCreationStatus.SUCCESS : ExtensionWebSocketCreationStatus.CONNECTION_FAILED;
    }

    // it is done for a reason
    @Override
    public int decIn() {
        return inCount.incrementAndGet();
    }

    @Override
    public int decOut() {
        return outCount.decrementAndGet();
    }

    @Override
    public int incIn() {
        return inCount.decrementAndGet();
    }

    @Override
    public int incOut() {
        return outCount.incrementAndGet();
    }

    @Override
    public int in() {
        return inCount.get();
    }

    @Override
    public int out() {
        return outCount.get();
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
                        if (webSocketPool.isEmpty()) {
                            continue;
                        }

                        int index = roundRobinIndex.getAndUpdate(
                                val -> (val + 1) % webSocketPool.size()
                        );

                        TurboExtensionWebSocket socket = webSocketPool.get(index);

                        ConnectionMessage nextMsg = outQueue.poll();
                        if (nextMsg == null) continue;
                        nextMsg.setHash(Utilities.generateID(index + 1, counterMap.getAndIncrement(index)));

                        ByteBuffer arr = ByteBuffer.allocate(1 + Long.BYTES + nextMsg.getMessageByteBuffer().remaining())
                                .put((byte) nextMsg.getCode().value)
                                .putLong(nextMsg.getPayloadLength())
                                .put(nextMsg.getMessageByteBuffer().duplicate());
                        arr.flip();
                        socket.sendByteBuffer(arr);
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

    public void close() {
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
