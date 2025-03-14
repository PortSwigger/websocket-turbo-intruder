package embedded;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConnectionQueueConsumer implements Runnable {

    private final Consumer<ConnectionWrapper> connectionProcessor;
    private final EmbeddedServerStatus serverStatus;
    private final Supplier<ConnectionWrapper> pendingConnectionSupplier;

    public ConnectionQueueConsumer(
            Consumer<ConnectionWrapper> connectionProcessor,
            EmbeddedServerStatus attackStatus,
            Supplier<ConnectionWrapper> pendingConnectionSupplier) {
        this.connectionProcessor = connectionProcessor;
        this.serverStatus = attackStatus;
        this.pendingConnectionSupplier = pendingConnectionSupplier;
    }

    @Override
    public void run() {
        while (serverStatus.isRunning()) {
            ConnectionWrapper connection = pendingConnectionSupplier.get();

            if (connection != null) {
                // TODO check status
                connectionProcessor.accept(connection);
            }
        }
    }
}
