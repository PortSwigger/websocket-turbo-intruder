package connection;

import burp.WebSocketExtensionWebSocketMessageHandler;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.WebSockets;
import burp.api.montoya.websocket.extension.ExtensionWebSocket;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreation;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import data.AttackIdAndWebSocketConnectionMessage;
import data.ConnectionMessage;
import data.PendingMessages;
import data.WebSocketConnectionMessage;
import logger.Logger;
import logger.LoggerLevel;
import socket.TurboWebSocketInterface;
import socket.TurboWebSocketMessagePublisher;
import utils.PayloadManipulator;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;

public class WebSocketConnection implements Connection, TurboWebSocketInterface {
    public final AtomicInteger outCounter = new AtomicInteger(0);
    private final Logger logger;
    private final WebSockets webSockets;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final HttpRequest upgradeRequest;
    private final ExtensionWebSocket extensionWebSocket;
    private final PayloadManipulator payloadManipulator;

    public WebSocketConnection(
            Logger logger,
            WebSockets webSockets,
            PendingMessages pendingMessages,
            int attackId,
            HttpRequest upgradeRequest,
            TurboWebSocketMessagePublisher publisher
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
    public void queue(String payload) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCounter.getAndIncrement(), payload, CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(int code, byte[] payload) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCounter.getAndIncrement(), new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(int code, byte[] payload, int length) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCounter.getAndIncrement(), new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(int code, byte[] payload, String length) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCounter.getAndIncrement(), new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(String payload, String replacement) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCounter.getAndIncrement(), newPayload, CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queueWithComment(String payload, String comment) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCounter.getAndIncrement(), payload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void queueWithComment(String payload, String replacement, String comment) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new WebSocketConnectionMessage(outCounter.getAndIncrement(), newPayload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void sendMessage(ConnectionMessage message) {
        extensionWebSocket.sendTextMessage(message.getMessage());
    }

    @Override
    public HttpRequest upgradeRequest() {
        return upgradeRequest;
    }

    public void sendMessage(String payload) {
        extensionWebSocket.sendTextMessage(payload);
    }

    private ExtensionWebSocket createExtensionWebSocket(HttpRequest upgradeRequest) throws Exception {
        ExtensionWebSocket extensionWebSocket;

        ExtensionWebSocketCreation extensionWebSocketCreation = webSockets.createWebSocket(upgradeRequest);
        logger.logOutput(LoggerLevel.DEBUG, "WebSocketConnection Upgrade request: " + upgradeRequest.toString());

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

    @Override
    public boolean isOpen() {
        return extensionWebSocket != null;
    }

    @Override
    public ExtensionWebSocketCreationStatus status() {
        return ExtensionWebSocketCreationStatus.SUCCESS;
    }

    public void stop() {
        if (extensionWebSocket != null) extensionWebSocket.close();
    }
}
