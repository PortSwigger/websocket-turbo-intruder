package connection;

import burp.WebSocketExtensionWebSocketMessageHandler;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.WebSockets;
import burp.api.montoya.websocket.extension.ExtensionWebSocket;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreation;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import data.*;
import logger.Logger;
import logger.LoggerLevel;
import socket.TurboConnectionPublisher;
import utils.PayloadManipulator;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;

public class WebSocketConnection implements Connection {
    public final AtomicInteger inCount = new AtomicInteger(0);
    public final AtomicInteger outCount = new AtomicInteger(0);
    private final Logger logger;
    private final WebSockets webSockets;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final HttpRequest upgradeRequest;
    private final ExtensionWebSocket extensionWebSocket;
    private final PayloadManipulator payloadManipulator;
    private ExtensionWebSocketCreation extensionWebSocketCreation;

    public WebSocketConnection(
            Logger logger,
            WebSockets webSockets,
            PendingMessages pendingMessages,
            int attackId,
            HttpRequest upgradeRequest,
            TurboConnectionPublisher publisher
    ) throws Exception {
        this.logger = logger;
        this.webSockets = webSockets;
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
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(i, payload, CLIENT_TO_SERVER, this)));
        return i;
    }

    @Override
    public int queue(byte[] payload) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(i, new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
        return i;
    }

    @Override
    public int queue(FrameType code, byte[] payload) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(i, new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
        return i;
    }

    @Override
    public int queue(FrameType code, byte[] payload, int length) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(i, new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
        return i;
    }

    @Override
    public int queue(FrameType code, byte[] payload, String length) {
        int i = outCount.getAndIncrement();
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(i, new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
        return i;
    }

    @Override
    public int queue(String payload, String replacement) {
        int i = outCount.getAndIncrement();
        String newPayload = payloadManipulator.modify(payload, replacement);
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(i, newPayload, CLIENT_TO_SERVER, this)));
        return i;
    }

    @Override
    public void queueWithComment(String payload, String comment) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCount.getAndIncrement(), payload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void queueWithComment(String payload, String replacement, String comment) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCount.getAndIncrement(), newPayload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void sendMessage(ConnectionMessage message) {
        extensionWebSocket.sendTextMessage(message.getMessage());
    }

    @Override
    public HttpRequest upgradeRequest() {
        return upgradeRequest;
    }

    @Override
    public ExtensionWebSocketCreationStatus status() {
        return extensionWebSocketCreation.status();
    }

    @Override
    public int decIn() {
        return inCount.decrementAndGet();
    }

    @Override
    public int decOut() {
        return outCount.decrementAndGet();
    }

    @Override
    public int incIn() {
        return inCount.getAndIncrement();
    }

    @Override
    public int incOut() {
        return outCount.getAndIncrement();
    }

    @Override
    public int in() {
        return inCount.get();
    }

    @Override
    public int out() {
        return outCount.get();
    }

    private ExtensionWebSocket createExtensionWebSocket(HttpRequest upgradeRequest) throws Exception {
        ExtensionWebSocket extensionWebSocket;

        extensionWebSocketCreation = webSockets.createWebSocket(upgradeRequest);
        if (extensionWebSocketCreation.webSocket().isPresent()) {
            extensionWebSocket = extensionWebSocketCreation.webSocket().get();

            extensionWebSocket.registerMessageHandler(
                    new WebSocketExtensionWebSocketMessageHandler(
                            logger,
                            pendingMessages,
                            attackId,
                            this // TODO
                    )
            );
        } else {
            logger.logOutput(LoggerLevel.UI, "Failed to create websocket connection: " + extensionWebSocketCreation.status());
            throw new Exception("Failed to create websocket connection");
        }

        return extensionWebSocket;
    }

    public void close() {
        if (extensionWebSocket != null) {
            logger.logOutput(LoggerLevel.UI, "Connection was closed by server!");
            extensionWebSocket.close();
        }
    }
}
