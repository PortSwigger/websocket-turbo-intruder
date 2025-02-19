package connection;

import burp.api.montoya.http.message.requests.HttpRequest;
import data.AttackIdAndWebSocketConnectionMessage;
import data.ConnectionMessage;
import data.PendingMessages;
import data.RawWebsocketConfig;
import logger.Logger;
import logger.LoggerLevel;
import socket.TurboWebSocketInterface;
import socket.TurboWebSocketMessagePublisher;
import turbo.*;
import utils.PayloadManipulator;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;

public class RawWebSocketConnection implements Connection, TurboWebSocketInterface {
    public final AtomicInteger outCounter = new AtomicInteger(0);
    private final Logger logger;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final RawWebSockets webSockets;
    private final HttpRequest upgradeRequest;
    private final RawExtensionWebSocket extensionWebSocket;
    private final PayloadManipulator payloadManipulator;

    public RawWebSocketConnection(
            Logger logger,
            PendingMessages pendingMessages,
            int attackId,
            HttpRequest upgradeRequest,
            TurboWebSocketMessagePublisher publisher,
            RawWebsocketConfig config
    ) throws Exception {
        this.logger = logger;
        this.webSockets = new RawWebSockets(logger, config);
        this.pendingMessages = pendingMessages;
        this.attackId = attackId;
        this.upgradeRequest = upgradeRequest;
        this.payloadManipulator = new PayloadManipulator();
        publisher.addSubscriber(this);

        extensionWebSocket = createExtensionWebSocket(upgradeRequest);
    }

    @Override
    public void queue(String payload) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new RawWebSocketConnectionMessage(outCounter.getAndIncrement(), payload, CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(int code, byte[] payload) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new RawWebSocketConnectionMessage(outCounter.getAndIncrement(), new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(int code, byte[] payload, int length) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new RawWebSocketConnectionMessage(outCounter.getAndIncrement(), new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(int code, byte[] payload, String length) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new RawWebSocketConnectionMessage(outCounter.getAndIncrement(), new String(payload, Charset.defaultCharset()), CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queue(String payload, String replacement) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new RawWebSocketConnectionMessage(outCounter.getAndIncrement(), newPayload, CLIENT_TO_SERVER, this)));
    }

    @Override
    public void queueWithComment(String payload, String comment) {
        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new RawWebSocketConnectionMessage(outCounter.getAndIncrement(), payload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void queueWithComment(String payload, String replacement, String comment) {
        String newPayload = payloadManipulator.modify(payload, replacement);

        pendingMessages.accept(new AttackIdAndWebSocketConnectionMessage(attackId, new RawWebSocketConnectionMessage(outCounter.getAndIncrement(), newPayload, CLIENT_TO_SERVER, comment, this)));
    }

    @Override
    public void sendMessage(ConnectionMessage message) {
        extensionWebSocket.sendTextMessage(message.getMessage());
    }

    @Override
    public HttpRequest upgradeRequest() {
        return upgradeRequest;
    }


    private RawExtensionWebSocket createExtensionWebSocket(HttpRequest upgradeRequest) throws Exception {
        RawExtensionWebSocket extensionWebSocket;

        RawExtensionWebSocketCreation extensionWebSocketCreation = webSockets.createWebSocket(upgradeRequest);
        logger.logOutput(LoggerLevel.DEBUG, "Raw Websocket Upgrade request: " + upgradeRequest);

        if (extensionWebSocketCreation.webSocket().isPresent()) {
            extensionWebSocket = (RawExtensionWebSocket) extensionWebSocketCreation.webSocket().get();

            extensionWebSocket.registerMessageHandler(
                    new RawWebSocketExtensionWebSocketMessageHandler(
                            logger,
                            pendingMessages,
                            attackId,
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
