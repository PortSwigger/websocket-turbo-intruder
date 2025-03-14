package embedded;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TableConnectionQueueConsumer implements Runnable {
    private final Supplier<ConnectionWrapper> queue;
    private final Consumer<ConnectionWrapper> messageConsumer;
    private final EmbeddedServerStatus serverStatus;

    public TableConnectionQueueConsumer(
            Supplier<ConnectionWrapper> queue,
            EmbeddedServerStatus serverStatus,
            Consumer<ConnectionWrapper> messageConsumer
    ) {
        this.queue = queue;
        this.messageConsumer = messageConsumer;
        this.serverStatus = serverStatus;
    }

    @Override
    public void run() {
        while (serverStatus.isRunning()) {
            ConnectionWrapper connectionMessage = queue.get();

            if (connectionMessage != null) { // TODO - push down
                messageConsumer.accept(connectionMessage);
            }
        }
    }
}
