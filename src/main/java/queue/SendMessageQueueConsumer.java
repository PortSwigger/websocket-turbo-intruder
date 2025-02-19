package queue;

import attack.AttackStatus;
import burp.api.montoya.collaborator.CollaboratorClient;
import burp.api.montoya.collaborator.Interaction;
import data.ConnectionMessage;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;
import static java.lang.Thread.sleep;

public class SendMessageQueueConsumer implements Runnable {
    private final Consumer<ConnectionMessage> messageProcessor;
    private final AttackStatus attackStatus;
    private final Supplier<ConnectionMessage> pendingMessageSupplier;

    public SendMessageQueueConsumer(
            Consumer<ConnectionMessage> messageProcessor,
            AttackStatus attackStatus,
            Supplier<ConnectionMessage> pendingMessageSupplier
    ) {
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
                if (webSocketConnectionMessage.getDirection() == CLIENT_TO_SERVER) webSocketConnectionMessage.send();
                messageProcessor.accept(webSocketConnectionMessage);
            }
        }
    }
}
