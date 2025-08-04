package ui.attack;

import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import data.ConnectionMessage;
import ui.attack.table.WebSocketMessageTable;
import ui.attack.table.WebSocketMessageTableModel;
import utils.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static burp.api.montoya.core.ByteArray.byteArray;
import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;
import static java.awt.BorderLayout.CENTER;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;

public class WebSocketLoggerPanel extends JPanel {
    private final UserInterface userInterface;
    private final WebSocketMessageTableModel tableModel;
    private final IconFactory iconFactory;

    public WebSocketLoggerPanel(
            UserInterface userInterface,
            WebSocketMessageTableModel tableModel
    ) {
        super(new BorderLayout());
        this.userInterface = userInterface;
        this.tableModel = tableModel;
        this.iconFactory = new IconFactory(userInterface);

        initComponents();
    }

    private void initComponents() {
        WebSocketMessageEditor webSocketMessageEditor = userInterface.createWebSocketMessageEditor(READ_ONLY);

        Consumer<ConnectionMessage> selectedMessageConsumer = connectionMessage -> {
            webSocketMessageEditor.setContents(byteArray(connectionMessage.getMessage()));
        };

        JSplitPane attackPanelSplitPane = new JSplitPane(
                VERTICAL_SPLIT,
                new WebSocketMessageTable(userInterface, tableModel, iconFactory, selectedMessageConsumer),
                webSocketMessageEditor.uiComponent()
        );
        attackPanelSplitPane.setResizeWeight(0.5);
        userInterface.applyThemeToComponent(attackPanelSplitPane);
        this.add(attackPanelSplitPane, CENTER);

    }

}
