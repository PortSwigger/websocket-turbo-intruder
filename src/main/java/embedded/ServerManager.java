package embedded;

import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.websocket.WebSockets;
import data.ConnectionMessage;
import data.InitialWebSocketMessage;
import logger.Logger;

import java.util.function.Consumer;

public class ServerManager implements ServerStarter {
    private final Logger logger;
    private final Consumer<ConnectionMessage> historyMessageConsumer;
    private final Consumer<ConnectionWrapper> connectionsConsumer;
    private final TurboTableItemList wsItems;
    private final WebSockets websockets;
    private final ServerStatus serverStatus;
    private JettyEmbeddedServerExecutor serverExecutor;
    private ServerDetails serverDetails;

    public ServerManager(
            Logger logger,
            WebSockets websockets,
            TurboTableItemList wsItems,
            Consumer<ConnectionMessage> historyMessageConsumer,
            Consumer<ConnectionWrapper> connectionsConsumer,
            ServerStatus serverStatus) {
        this.logger = logger;
        this.historyMessageConsumer = historyMessageConsumer;
        this.websockets = websockets;
        this.wsItems = wsItems;
        this.serverStatus = serverStatus;
        this.connectionsConsumer = connectionsConsumer;
    }

    public void startServer(ServerDetails serverDetails) {
        this.serverDetails = serverDetails;
        serverStatus.start();
        serverExecutor = new JettyEmbeddedServerExecutor(logger, serverDetails);
        serverExecutor.start();

        for (TurboTableItem item : wsItems.getItems()) {
            if (item instanceof WebSocketsTurboItem wsItem) {
                try {
                    ConnectionWrapper client = new ConnectionWrapper(
                            logger,
                            historyMessageConsumer,
                            websockets,
                            wsItem.getUpgradeRequest(),
                            wsItem.getMessages().stream().map(InitialWebSocketMessage::message).toList(),
                            serverDetails.getScript()
                    );
                    serverExecutor.processConnection(client);
                    connectionsConsumer.accept(client);
                } catch (Exception e) {
                    try {
                        logger.logError("Connection to " + wsItem.getUpgradeRequest().url() + " failed.");
                    } catch (MalformedRequestException exception) {
                        logger.logError(exception.getMessage());
                    }

                }
            }
        }
    }

    @Override
    public String uri() {
        return serverDetails == null ? "" : String.format("http://127.0.0.1:%d/proxy", serverDetails.getPort());
    }

    public void stopServer() {
        serverStatus.stop();
        if (serverExecutor != null) serverExecutor.stop();
    }

    public String secret() {
        return serverDetails == null ? "" : serverDetails.getSecret();
    }
}
