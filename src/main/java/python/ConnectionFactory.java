package python;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.WebSockets;
import connection.*;
import data.Engine;
import data.PendingMessages;
import data.RawWebsocketConfig;
import logger.Logger;
import socket.TurboConfig;
import socket.TurboExtensionWebSocket;
import socket.TurboWebSocketMessagePublisher;
import socket.TurboWebSockets;

public class ConnectionFactory {
    private final Logger logger;
    private final WebSockets webSockets;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final TurboWebSocketMessagePublisher publisher;

    public ConnectionFactory(
            Logger logger,
            WebSockets webSockets,
            PendingMessages pendingMessages,
            int attackId,
            TurboWebSocketMessagePublisher publisher
    ) {
        this.logger = logger;
        this.webSockets = webSockets;
        this.pendingMessages = pendingMessages;
        this.attackId = attackId;
        this.publisher = publisher;
    }

    private Connection connection(HttpRequest upgradeRequest) throws Exception {
        if (webSockets instanceof TurboWebSockets)
            return new TurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, TurboConfig.turbo());
        return new WebSocketConnection(logger, webSockets, pendingMessages, attackId, upgradeRequest, publisher);
    }

    @SuppressWarnings("unused") // called by Python
    public Connection create(HttpRequest upgradeRequest) throws Exception {
        return connection(upgradeRequest);
    }

    @SuppressWarnings("unused") // called by Python
    public Connection create(HttpRequest upgradeRequest, Engine engine, TurboConfig config) throws Exception {
        switch (engine) {
            case RAW -> {
                return new RawWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher,
                        new RawWebsocketConfig(Engine.RAW, config.connectionTimeout(), config().isPinging()));
            }
            case TURBO -> {
                return new TurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, config);
            }
            case MULTI -> {
                return new MultiThreadTurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, config);
            }
            case null, default -> {
                return connection(upgradeRequest);
            }
        }
    }
    public Connection create(HttpRequest upgradeRequest, Engine engine) throws Exception {
        switch (engine) {
            case TURBO -> {
                return new TurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, TurboConfig.turbo());
            }
            case MULTI -> {
                return new MultiThreadTurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, TurboConfig.multi());
            }
            case null, default -> {
                return connection(upgradeRequest);
            }
        }
    }

    @SuppressWarnings("unused") // called by Python
    public TurboConfig config() {
        return new TurboConfig();
    }

    @SuppressWarnings("unused") // called by Python
    public TurboConfig config(int connectionTimeout, int sendingInterval, int retries, int nThreads, String helloMessage, boolean isPinging, boolean useChunks) {
        return new TurboConfig(connectionTimeout, sendingInterval, retries, nThreads, helloMessage, isPinging, useChunks);
    }
}
