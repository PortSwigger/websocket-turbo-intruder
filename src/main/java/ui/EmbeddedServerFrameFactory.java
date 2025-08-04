package ui;

import burp.api.montoya.ai.Ai;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.websocket.WebSockets;
import config.ExtensionConfiguration;
import data.InitialWebSocketMessage;
import embedded.*;
import logger.Logger;
import ui.attack.table.WebSocketMessageTableModel;
import ui.editor.AiManager;
import ui.editor.embedded.EmbeddedServerFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.stream.Collectors;

public class EmbeddedServerFrameFactory {
    private final Ai ai;
    private final Logger logger;
    private final ExtensionConfiguration extensionConfiguration;
    private final Organizer organizer;
    private final UserInterface userInterface;
    private final WebSockets websockets;
    private final WebSocketMessageTableModel historyTableModel;

    public EmbeddedServerFrameFactory(
            Ai ai,
            Logger logger,
            ExtensionConfiguration extensionConfiguration,
            Organizer organizer,
            UserInterface userInterface,
            WebSockets websockets,
            WebSocketMessageTableModel historyTableModel
    ) {
        this.ai = ai;
        this.extensionConfiguration = extensionConfiguration;
        this.logger = logger;
        this.organizer = organizer;
        this.userInterface = userInterface;
        this.websockets = websockets;
        this.historyTableModel = historyTableModel;
    }
    private static boolean isValidRequest(HttpRequest request) {
        try {
            request.url();
            return true;
        } catch (MalformedRequestException e) {
            return false;
        }
    }

    public EmbeddedServerFrame from(List<InitialWebSocketMessage> webSocketMessages) {

        ServerStatus serverStatus = new ServerStatus();

        WrappedConnectionTableModel connectionTableModel = new WrappedConnectionTableModel();

        TurboTableItemList wsItems = new TurboTableItemList();
        List<InitialWebSocketMessage> safeMessages = webSocketMessages.stream()
                .filter(msg -> isValidRequest(msg.upgradeRequest()))
                .toList();

        safeMessages.stream()
                .collect(Collectors.groupingBy(InitialWebSocketMessage::upgradeRequest))
                .forEach((key, value) ->
                        wsItems.addItem(new WebSocketsTurboItem(key, value)));

        TurboTableModel tableModel = new TurboTableModel(wsItems);
        AiManager aiManager = new AiManager(logger, ai);

        ServerManager serverManager = new ServerManager(
                logger,
                websockets,
                wsItems,
                historyTableModel::add,
                connectionTableModel::add,
                serverStatus
        );

        EmbeddedServerFrame embeddedFrame = new EmbeddedServerFrame(
                logger,
                organizer,
                userInterface,
                aiManager,
                extensionConfiguration,
                serverManager,
                serverStatus,
                tableModel,
                connectionTableModel
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
