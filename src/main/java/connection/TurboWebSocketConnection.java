package connection;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.Direction;
import data.AttackIdAndWebSocketConnectionMessage;
import data.ConnectionMessage;
import data.PendingMessages;
import logger.Logger;
import logger.LoggerLevel;
import socket.*;
import utils.PayloadManipulator;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;
import static java.time.LocalDateTime.now;

public class TurboWebSocketConnection implements Connection, TurboWebSocketInterface {
    public final AtomicInteger outCounter = new AtomicInteger(0);
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
            TurboWebSocketMessagePublisher publisher,
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
                now(), this, length ));
    }

    /**
     * Centralized method to enqueue a message into pendingMessages.
     */
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
        ByteBuffer arr = ByteBuffer.allocate(1 + Long.BYTES + message.getMessageByteBuffer().remaining())
                .put((byte) message.getCode())
                .putLong(message.getPayloadLength())
                .put(message.getMessageByteBuffer().duplicate());
        arr.flip();
        extensionWebSocket.sendByteBuffer(arr);
//        extensionWebSocket.sendBinaryMessage(
//                ByteArray.byteArray(message.getCode())
//                        .withAppended(ByteBuffer.allocate(Long.BYTES).putLong(message.getPayloadLength()).array())
//                        .withAppended(message.getMessageByteBuffer().array()));
    }

    @Override
    public HttpRequest upgradeRequest() {
        return upgradeRequest;
    }

    public void sendMessage(String payload) {
        extensionWebSocket.sendTextMessage(payload);
    }

    public void sendBinary(ByteArray payload) {
        extensionWebSocket.sendBinaryMessage(payload);
    }

    private TurboExtensionWebSocket createExtensionWebSocket(HttpRequest upgradeRequest) throws Exception {
        TurboExtensionWebSocket extensionWebSocket;

        TurboExtensionWebSocketCreation extensionWebSocketCreation = webSockets.createWebSocket(upgradeRequest);
        logger.logOutput(LoggerLevel.DEBUG, "Raw Websocket Upgrade request: " + upgradeRequest);

        if (extensionWebSocketCreation.webSocket().isPresent()) {
            extensionWebSocket = (TurboExtensionWebSocket) extensionWebSocketCreation.webSocket().get();

            extensionWebSocket.registerMessageHandler(
                    new TurboWebSocketExtensionWebSocketMessageHandler(
                            logger,
                            pendingMessages,
                            attackId,
                            0,
                            this // TODO
                    )
            );
        } else {
            logger.logError("Failed to create websocket connection");
            throw new Exception("Failed to create websocket connection");
        }

        return extensionWebSocket;
    }

    public void stop() {
        if (extensionWebSocket != null) extensionWebSocket.close();
    }
}
