package embedded;

import logger.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WebSocketsToConnect implements Consumer<ConnectionWrapper>, Supplier<ConnectionWrapper> {
    private final Logger logger;
    private final BlockingQueue<ConnectionWrapper> clients;
    private final EmbeddedServerStatus serverStatus;

    public WebSocketsToConnect(Logger logger, EmbeddedServerStatus serverStatus) {
        this.logger = logger;
        this.serverStatus = serverStatus;
        this.clients = new LinkedBlockingQueue<>();
    }

    @Override
    public void accept(ConnectionWrapper client) {
        clients.add(client);
    }

    @Override
    public ConnectionWrapper get() {
        try {
            return clients.take();
        } catch (InterruptedException e) {
            if (serverStatus.isRunning()) {
                logger.logError("Error taking from clients to display.");
            }

            return null;
        }
    }

    public void clear() {
        clients.clear();
    }
}
