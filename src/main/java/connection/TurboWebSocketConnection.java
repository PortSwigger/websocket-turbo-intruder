package connection;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import data.AttackIdAndWebSocketConnectionMessage;
import data.ConnectionMessage;
import data.FrameType;
import data.PendingMessages;
import logger.Logger;
import logger.LoggerLevel;
import socket.*;
import utils.PayloadManipulator;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;
import static java.time.LocalDateTime.now;

public class TurboWebSocketConnection implements Connection {
    public final AtomicInteger inCount = new AtomicInteger(0);
    public final AtomicInteger outCount = new AtomicInteger(0);
    private final Logger logger;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final TurboWebSockets webSockets;
    private final HttpRequest upgradeRequest;
    private final TurboExtensionWebSocket extensionWebSocket;
    private final PayloadManipulator payloadManipulator;

    public TurboWebSocketConnection(
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
        this.payloadManipulator = new PayloadManipulator();
        publisher.addSubscriber(this);

        extensionWebSocket = createExtensionWebSocket(upgradeRequest);
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
        ByteBuffer arr = ByteBuffer.allocate(1 + Long.BYTES + message.getMessageByteBuffer().remaining())
                .put((byte) message.getCode().value)
                .putLong(message.getPayloadLength())
                .put(message.getMessageByteBuffer().duplicate());
        arr.flip();
        extensionWebSocket.sendByteBuffer(arr);
    }

    @Override
    public HttpRequest upgradeRequest() {
        return upgradeRequest;
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


    private TurboExtensionWebSocket createExtensionWebSocket(HttpRequest upgradeRequest) throws Exception {
        TurboExtensionWebSocket extensionWebSocket;

        TurboExtensionWebSocketCreation extensionWebSocketCreation = webSockets.createWebSocket(upgradeRequest);

        if (extensionWebSocketCreation.webSocket().isPresent()) {
            extensionWebSocket = (TurboExtensionWebSocket) extensionWebSocketCreation.webSocket().get();

            extensionWebSocket.registerMessageHandler(
                    new TurboWebSocketExtensionWebSocketMessageHandler(
                            logger,
                            pendingMessages,
                            attackId,
                            0,
                            this
                    )
            );
        } else {
            logger.logError("Failed to create websocket connection");
            throw new Exception("Failed to create websocket connection");
        }

        return extensionWebSocket;
    }


    @Override
    public ExtensionWebSocketCreationStatus status() {
        return extensionWebSocket.getStatus();
    }

    public void close() {
        if (extensionWebSocket != null) extensionWebSocket.close();
    }
}
