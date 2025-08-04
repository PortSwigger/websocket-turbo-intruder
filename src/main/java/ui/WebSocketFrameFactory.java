package ui;

import attack.AttackManager;
import attack.AttackScriptExecutor;
import attack.AttackState;
import burp.api.montoya.ai.Ai;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.websocket.WebSockets;
import config.ExtensionConfiguration;
import data.InitialWebSocketMessage;
import data.MessagesToDisplay;
import data.PendingMessages;
import logger.Logger;
import python.ConnectionFactoryFactory;
import socket.TurboConnectionPublisher;
import ui.attack.table.WebSocketMessageTableModel;
import ui.editor.AiManager;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WebSocketFrameFactory {
    private final Logger logger;
    private final UserInterface userInterface;
    private final ExtensionConfiguration extensionConfiguration;
    private final WebSockets webSockets;
    private final Ai ai;
    private final TurboConnectionPublisher publisher;
    private final WebSocketMessageTableModel historyTableModel;

    public WebSocketFrameFactory(
            Logger logger,
            UserInterface userInterface,
            ExtensionConfiguration extensionConfiguration,
            WebSockets webSockets,
            Ai ai,
            TurboConnectionPublisher publisher,
            WebSocketMessageTableModel historyTableModel
    ) {
        this.logger = logger;
        this.userInterface = userInterface;
        this.extensionConfiguration = extensionConfiguration;
        this.webSockets = webSockets;
        this.ai = ai;
        this.publisher = publisher;
        this.historyTableModel = historyTableModel;
    }

    public WebSocketFrame from(InitialWebSocketMessage webSocketMessage) {
        AttackState attackState = new AttackState();

        MessagesToDisplay messagesToDisplay = new MessagesToDisplay(logger, attackState);
        PendingMessages pendingMessages = new PendingMessages(logger, attackState);

        AttackScriptExecutor scriptExecutor = new AttackScriptExecutor(
                logger,
                publisher,
                messagesToDisplay,
                new ConnectionFactoryFactory(
                        logger,
                        webSockets,
                        pendingMessages,
                        attackState::currentAttackId,
                        publisher
                )
        );

        WebSocketMessageTableModel webSocketMessageTableModel = new WebSocketMessageTableModel();

        AttackManager attackManager = new AttackManager(
                logger,
                pendingMessages,
                messagesToDisplay,
                historyTableModel::add,
                webSocketMessageTableModel::add,
                scriptExecutor,
                attackState
        );
        AiManager aiManager = new AiManager(logger, ai);

        WebSocketFrame webSocketFrame = new WebSocketFrame(
                logger,
                userInterface,
                aiManager,
                extensionConfiguration,
                webSocketMessage,
                webSocketMessageTableModel,
                attackManager,
                attackState
        );

        webSocketFrame.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        attackManager.stopAttack();
                        aiManager.stop();
                    }
                }
        );

        return webSocketFrame;
    }
}
