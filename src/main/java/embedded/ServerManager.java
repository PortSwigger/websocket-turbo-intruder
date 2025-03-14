package embedded;

import data.InitialWebSocketMessage;
import logger.Logger;
import turbo.TurboThreadExecutor;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.time.LocalTime.now;
import static logger.LoggerLevel.UI;

public class ServerManager {
    private final Logger logger;
    private final Consumer<ConnectionWrapper> connectionsConsumer;
    private final WebSocketsToConnect webSocketsToConnect;
    private final EmbeddedServerExecutor serverExecutor;
    private final List<InitialWebSocketMessage> webSocketMessages;
    private EmbeddedServerState serverState;
    private TurboThreadExecutor sendMessageExecutorService;
    private TurboThreadExecutor connectionExecutorService;

    public ServerManager(
            Logger logger,
            List<InitialWebSocketMessage> webSocketMessages,
            WebSocketsToConnect webSocketsToConnect,
            Consumer<ConnectionWrapper> connectionsConsumer,
            EmbeddedServerExecutor serverExecutor,
            EmbeddedServerState serverState) {
        this.logger = logger;
        this.webSocketMessages = webSocketMessages;
        this.connectionsConsumer = connectionsConsumer;
        this.webSocketsToConnect = webSocketsToConnect;
        this.serverExecutor = serverExecutor;
        this.serverState = serverState;
    }

    public void startServer() {
        serverState.startServer();

        serverExecutor.startServer();

        String message = String.format("[%s] Embedded server started.",
                now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
        logger.logOutput(UI, message);

        sendMessageExecutorService = new TurboThreadExecutor(logger);
        sendMessageExecutorService.submitTask(
                new ConnectionQueueConsumer(
                        serverExecutor::processConnection,
                        serverState,
                        webSocketsToConnect
                )
        );

        connectionExecutorService = new TurboThreadExecutor(logger);
        connectionExecutorService.submitTask(new Runnable() {
            @Override
            public void run() {
                if (serverState.isRunning()) {
                    Map<String, List<InitialWebSocketMessage>> connections = webSocketMessages.stream()
                            .collect(Collectors.groupingBy(
                                    msg -> msg.upgradeRequest().url(),
                                    Collectors.toList()
                            ));
                    for (Map.Entry<String, List<InitialWebSocketMessage>> entry : connections.entrySet()) {
                        try {
                            if (!entry.getValue().isEmpty()) {
                                InitialWebSocketMessage initialWebSocketMessage = entry.getValue().getFirst();
                                ConnectionWrapper client = new ConnectionWrapper(
                                        logger,
                                        initialWebSocketMessage.upgradeRequest(),
                                        entry.getValue().stream().map(InitialWebSocketMessage::message).toList(),
                                        new ArrayList<>());
                                webSocketsToConnect.accept(client);
                                connectionsConsumer.accept(client);
                            }
                        } catch (Exception e) {
                            logger.logError("Connection failed");
                        }
                    }
                } else {
                    logger.logError("Server is not running. Can not create connection");
                }
            }
        });
    }

    public void stopServer() {
        serverState.stopServer();
        serverExecutor.stopServer();

        if (sendMessageExecutorService != null) sendMessageExecutorService.shutdown();
        if (connectionExecutorService != null) connectionExecutorService.shutdown();

        webSocketsToConnect.clear();
    }
}
