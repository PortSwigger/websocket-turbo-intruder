package ui.attack;

import attack.AttackManager;
import attack.AttackStatus;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import data.ConnectionMessage;
import ui.PanelSwitcher;
import ui.attack.table.TurboWebSocketMessageTable;
import ui.attack.table.WebSocketMessageTableModel;
import utils.IconFactory;
import utils.Pair;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import static burp.api.montoya.core.ByteArray.byteArray;
import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;
import static data.Progress.PROGRESS_BAR_STRING;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;

public class TurboWebSocketAttackPanel extends JPanel {
    private final UserInterface userInterface;
    private final AttackManager attackManager;
    private final AttackStatus attackStatus;
    private final PanelSwitcher panelSwitcher;
    private final WebSocketMessageTableModel tableModel;
    private final IconFactory iconFactory;

    public TurboWebSocketAttackPanel(
            UserInterface userInterface,
            AttackManager attackManager,
            AttackStatus attackStatus,
            PanelSwitcher panelSwitcher,
            WebSocketMessageTableModel tableModel
    ) {
        super(new BorderLayout());

        this.userInterface = userInterface;
        this.attackManager = attackManager;
        this.attackStatus = attackStatus;
        this.panelSwitcher = panelSwitcher;
        this.tableModel = tableModel;
        this.iconFactory = new IconFactory(userInterface);

        initComponents();
    }

    private static JPanel createLabeledComponent(String labelText, Component component) {
        JPanel panel = new JPanel(new BorderLayout());

        // Create the label
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.CENTER); // Center align the label
        panel.add(label, BorderLayout.NORTH);

        // Add the original component below the label
        panel.add(component, BorderLayout.CENTER);

        return panel;
    }

    private void initComponents() {
        WebSocketMessageEditor requestWebSocketMessageEditor = userInterface.createWebSocketMessageEditor(READ_ONLY);
        WebSocketMessageEditor responseWebSocketMessageEditor = userInterface.createWebSocketMessageEditor(READ_ONLY);
//        HttpRequestEditor upgradeRequestEditor = userInterface.createHttpRequestEditor(READ_ONLY);

        JSplitPane webSocketInformationDisplay = new JSplitPane(
                VERTICAL_SPLIT,
                createLabeledComponent("Request", requestWebSocketMessageEditor.uiComponent()),
                createLabeledComponent("Response", responseWebSocketMessageEditor.uiComponent())
        );
        webSocketInformationDisplay.setResizeWeight(0.5);

        Consumer<Pair<ConnectionMessage, ConnectionMessage>> selectedMessageConsumer = (connectionMessage) -> {
            requestWebSocketMessageEditor.setContents(connectionMessage.getFirst() == null ? byteArray("") : byteArray(connectionMessage.getFirst().getMessage()));
            responseWebSocketMessageEditor.setContents(connectionMessage.getSecond() == null ? byteArray("") : byteArray(connectionMessage.getSecond().getMessage()));
        };


        JSplitPane attackPanelSplitPane = new JSplitPane(
                HORIZONTAL_SPLIT,
                new TurboWebSocketMessageTable(userInterface, tableModel, iconFactory, selectedMessageConsumer),
                webSocketInformationDisplay
        );
        attackPanelSplitPane.setResizeWeight(0.5);

//        this.add(attackPanelSplitPane, CENTER);
        JSplitPane controlSplitPanel = new JSplitPane(
                VERTICAL_SPLIT,
                getLoggerComponent(),
                getHaltConfigureButton()
        );
        // Make the bottom component (button) non-resizable
        controlSplitPanel.setResizeWeight(1.0); // 1.0 means all extra space goes to the top component
        controlSplitPanel.setDividerSize(0); // No visible divider for aesthetics

        JSplitPane attackPanel = new JSplitPane(
                VERTICAL_SPLIT,
                attackPanelSplitPane,
                controlSplitPanel
        );
        attackPanel.setResizeWeight(0.9);
        this.add(attackPanel);
//        this.add(controlSplitPanel, SOUTH);
    }

    private Component getLoggerComponent() {

        JPanel panel = new JPanel(new BorderLayout());

        // Create a text area for displaying log messages
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false); // Make it read-only
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Automatically scroll to the bottom on new log messages
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Create Output Label (Bold Text)
        JLabel outputLabel = new JLabel("Output");
        outputLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        outputLabel.setFont(outputLabel.getFont().deriveFont(Font.BOLD));

        //
        JLabel logLabel = new JLabel(PROGRESS_BAR_STRING);
        logLabel.setHorizontalAlignment(SwingConstants.LEFT);
        Consumer<String> progressConsumer = message -> SwingUtilities.invokeLater(() -> {
            logLabel.setText(message);
        });


        JButton clearButton = getClearButton(textArea);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(outputLabel, BorderLayout.WEST);
        topPanel.add(logLabel, BorderLayout.CENTER);
        topPanel.add(clearButton, BorderLayout.EAST);

        // Add a scroll pane to the text area
        JScrollPane scrollPane = new JScrollPane(textArea);

        Consumer<String> messagesConsumer = message -> SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
        });

        attackManager.registerListener(messagesConsumer);
        attackManager.registerProgressListener(progressConsumer);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private static JButton getClearButton(JTextArea textArea) {
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Clear the JTextArea
                textArea.setText("");
            }
        });
        return clearButton;
    }

    private Component getHaltConfigureButton() {
        JButton haltConfigureButton = new JButton("Halt");
        haltConfigureButton.addActionListener(l -> {
            if (attackStatus.isRunning()) {
                attackManager.stopAttack();
                haltConfigureButton.setText("Configure");
            } else {
                tableModel.clear();

                haltConfigureButton.setText("Halt");
                panelSwitcher.showEditorPanel();
            }
        });

        return haltConfigureButton;
    }
}
