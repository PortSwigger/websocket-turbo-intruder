package socket;

import burp.api.montoya.websocket.BinaryMessage;
import burp.api.montoya.websocket.TextMessage;
import burp.api.montoya.websocket.extension.ExtensionWebSocketMessageHandler;
import connection.Connection;
import data.AttackIdAndWebSocketConnectionMessage;
import logger.Logger;
import utils.Utilities;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public class TurboWebSocketExtensionWebSocketMessageHandler implements ExtensionWebSocketMessageHandler {
    private final Logger logger;
    private final Consumer<AttackIdAndWebSocketConnectionMessage> pendingMessagesConsumer;
    private final int attackId;
    private final int ID;
    private final Connection connection;

    public TurboWebSocketExtensionWebSocketMessageHandler(
            Logger logger,
            Consumer<AttackIdAndWebSocketConnectionMessage> pendingMessagesConsumer,
            int attackId,
            int id,
            Connection connection
    ) //TODO can we hide the attack ID from this class
    {
        this.logger = logger;
        this.pendingMessagesConsumer = pendingMessagesConsumer;
        this.attackId = attackId;
        this.ID = id;
        this.connection = connection;
    }


    @Override
    public void textMessageReceived(TextMessage textMessage) {
        pendingMessagesConsumer.accept(
                new AttackIdAndWebSocketConnectionMessage(
                        attackId,
                        new TurboWebSocketConnectionMessage(
                                attackId,
                                textMessage.payload(),
                                textMessage.direction(),
                                connection
                        )
                )
        );
    }

    public void textMessageReceived(int id, TextMessage textMessage, String comment, LocalDateTime dateTime) {
        pendingMessagesConsumer.accept(
                new AttackIdAndWebSocketConnectionMessage(
                        attackId,
                        new TurboWebSocketConnectionMessage(
                                Utilities.generateID(ID, id - connection.in()),
                                textMessage.payload(),
                                textMessage.direction(),
                                comment,
                                dateTime,
                                connection
                        )
                )
        );
    }

    @Override
    public void binaryMessageReceived(BinaryMessage binaryMessage) {
        logger.logError("Unhandled binary message received");
    }

    @Override
    public void onClose() {
        connection.close();
    }
}
