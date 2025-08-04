package queue;

import attack.AttackStatus;
import data.ConnectionMessage;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;

public class SendMessageQueueConsumer implements Runnable {
    private final Consumer<ConnectionMessage> historyMessageProcessor;
    private final Consumer<ConnectionMessage> messageProcessor;
    private final AttackStatus attackStatus;
    private final Supplier<ConnectionMessage> pendingMessageSupplier;

    public SendMessageQueueConsumer(
            Consumer<ConnectionMessage> historyMessageProcessor,
            Consumer<ConnectionMessage> messageProcessor,
            AttackStatus attackStatus,
            Supplier<ConnectionMessage> pendingMessageSupplier
    ) {
        this.historyMessageProcessor = historyMessageProcessor;
        this.messageProcessor = messageProcessor;
        this.attackStatus = attackStatus;
        this.pendingMessageSupplier = pendingMessageSupplier;
    }

    @Override
    public void run() {
        while (attackStatus.isRunning()) {
            // TODO - Move into Will? (PendingMessages)
            ConnectionMessage webSocketConnectionMessage = pendingMessageSupplier.get();

            if (webSocketConnectionMessage != null) {
                // Logger
                historyMessageProcessor.accept(webSocketConnectionMessage);
                if (webSocketConnectionMessage.getDirection() == CLIENT_TO_SERVER) webSocketConnectionMessage.send();
                messageProcessor.accept(webSocketConnectionMessage);
            }
        }
    }
}
