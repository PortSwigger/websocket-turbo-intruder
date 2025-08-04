package python;

import burp.api.montoya.websocket.WebSockets;
import data.PendingMessages;
import logger.Logger;
import socket.TurboConnectionPublisher;

import java.util.function.Supplier;

public class ConnectionFactoryFactory {
    private final Logger logger;
    private final WebSockets webSockets;
    private final PendingMessages pendingMessages;
    private final Supplier<Integer> attackIdSupplier;
    private final TurboConnectionPublisher publisher;

    public ConnectionFactoryFactory(
            Logger logger,
            WebSockets webSockets,
            PendingMessages pendingMessages,
            Supplier<Integer> attackIdSupplier,
            TurboConnectionPublisher publisher
    ) {
        this.logger = logger;
        this.webSockets = webSockets;
        this.pendingMessages = pendingMessages;
        this.attackIdSupplier = attackIdSupplier;
        this.publisher = publisher;
    }

    public ConnectionFactory create() throws Exception {
        return new ConnectionFactory(logger, webSockets, pendingMessages, attackIdSupplier.get(), publisher);
    }
}
