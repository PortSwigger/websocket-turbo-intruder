package ui;

import burp.api.montoya.ui.UserInterface;
import data.InitialWebSocketMessage;
import embedded.*;
import logger.Logger;
import ui.embedded.EmbeddedFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class EmbeddedServerFrameFactory {
    private final Logger logger;
    private final UserInterface userInterface;

    public EmbeddedServerFrameFactory(
            Logger logger,
            UserInterface userInterface
    ) {
        this.logger = logger;
        this.userInterface = userInterface;
    }

    public EmbeddedFrame from(List<InitialWebSocketMessage> webSocketMessages) {
        EmbeddedServerState serverState = new EmbeddedServerState();

        WebSocketsToConnect webSocketsToConnect = new WebSocketsToConnect(logger, serverState);

        EmbeddedServerExecutor serverExecutor = new EmbeddedServerExecutor(logger, serverState);

        WrappedConnectionTableModel webSocketMessageTableModel = new WrappedConnectionTableModel();

        ServerManager serverManager = new ServerManager(
                logger,
                webSocketMessages,
                webSocketsToConnect,
                webSocketMessageTableModel::add,
                serverExecutor,
                serverState
        );

        EmbeddedFrame embeddedFrame = new EmbeddedFrame(
                logger,
                userInterface,
                webSocketMessageTableModel,
                serverManager,
                serverState
        );

        embeddedFrame.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        serverManager.stopServer();
                    }
                }
        );

        return embeddedFrame;
    }
}
