package burp;

import burp.api.montoya.websocket.BinaryMessage;
import burp.api.montoya.websocket.TextMessage;
import burp.api.montoya.websocket.extension.ExtensionWebSocketMessageHandler;
import connection.WebSocketConnection;
import data.AttackIdAndWebSocketConnectionMessage;
import data.WebSocketConnectionMessage;
import logger.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WebSocketExtensionWebSocketMessageHandler implements ExtensionWebSocketMessageHandler {
    public final AtomicInteger inCounter = new AtomicInteger(0);
    private final Logger logger;
    private final Consumer<AttackIdAndWebSocketConnectionMessage> pendingMessagesConsumer;
    private final int attackId;
    private final WebSocketConnection connection;

    public WebSocketExtensionWebSocketMessageHandler(
            Logger logger,
            Consumer<AttackIdAndWebSocketConnectionMessage> pendingMessagesConsumer,
            int attackId,
            WebSocketConnection connection
    ) //TODO can we hide the attack ID from this class
    {
        this.logger = logger;
        this.pendingMessagesConsumer = pendingMessagesConsumer;
        this.attackId = attackId;
        this.connection = connection;
    }

    @Override
    public void textMessageReceived(TextMessage textMessage) {
        pendingMessagesConsumer.accept(
                new AttackIdAndWebSocketConnectionMessage(
                        attackId,
                        new WebSocketConnectionMessage(
                                inCounter.getAndIncrement(),
                                textMessage.payload(),
                                textMessage.direction(),
                                connection
                        )
                )
        );
    }

    @Override
    public void binaryMessageReceived(BinaryMessage binaryMessage) {
        logger.logError("Unhandled binary message received");
    }
}