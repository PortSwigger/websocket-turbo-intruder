package ui.editor;

import attack.AttackStarter;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import config.ExtensionConfiguration;
import data.AttackDetails;
import data.InitialWebSocketMessage;
import logger.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import script.Script;
import script.ScriptLoaderFacade;
import ui.PanelSwitcher;
import utils.AiUtilities;
import utils.Utilities;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

import static java.awt.EventQueue.invokeLater;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;
import static utils.Utilities.setupFocusableButton;

public class WebSocketEditorPanel extends JPanel {
    private final UserInterface userInterface;
    private final InitialWebSocketMessage originalWebSocketMessage;
    private final ThemeAwareRSTAFactory rstaFactory;
    private final WebSocketEditorController controller;
    private final AiManager aiManager;
    private JComboBox<Script> scriptComboBox;
    private WebSocketMessageEditor webSocketsMessageEditor;
    private HttpRequestEditor upgradeHttpMessageEditor;
    private JSpinner numberOfThreadsSpinner;

    public WebSocketEditorPanel(
            AiManager aiManager,
            Logger logger,
            UserInterface userInterface,
            ExtensionConfiguration extensionConfiguration,
            AttackStarter attackStarter,
            InitialWebSocketMessage originalWebSocketMessage,
            PanelSwitcher panelSwitcher
    ) {
        super(new BorderLayout());

        this.userInterface = userInterface;
        this.originalWebSocketMessage = originalWebSocketMessage;
        ScriptLoaderFacade scriptLoader = new ScriptLoaderFacade(extensionConfiguration);
        this.rstaFactory = new ThemeAwareRSTAFactory(userInterface, logger);
        this.aiManager = aiManager;

        controller = new WebSocketEditorController(attackStarter, panelSwitcher, extensionConfiguration, scriptLoader);

        JSplitPane editableEditors = new JSplitPane(HORIZONTAL_SPLIT, getWebSocketMessageEditor(), getUpgradeHttpMessageEditor());

        JSplitPane splitPane = new JSplitPane(VERTICAL_SPLIT, editableEditors, getPythonCodeEditor());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (getWidth() > 0) {
                    editableEditors.setDividerLocation(0.5);
                    invokeLater(() -> splitPane.setDividerLocation(0.3));

//                    removeComponentListener(this);
                }
            }
        });

        this.add(splitPane, BorderLayout.CENTER);
    }

    private Component getWebSocketMessageEditor() {
        webSocketsMessageEditor = userInterface.createWebSocketMessageEditor();
        webSocketsMessageEditor.setContents(originalWebSocketMessage.message());

        return webSocketsMessageEditor.uiComponent();
    }

    private Component getUpgradeHttpMessageEditor() {
        upgradeHttpMessageEditor = userInterface.createHttpRequestEditor();
        upgradeHttpMessageEditor.setRequest(originalWebSocketMessage.upgradeRequest());

        return upgradeHttpMessageEditor.uiComponent();
    }

    private Component getPythonCodeEditor() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(getButtonPanel(), BorderLayout.NORTH);

        RSyntaxTextArea rSyntaxTextArea = rstaFactory.build();
        RTextScrollPane scrollableCodeEditor = new RTextScrollPane(rSyntaxTextArea);

        Script script = scriptComboBox.getItemAt(scriptComboBox.getSelectedIndex());
        String content = script == null ? "" : script.content();

        rSyntaxTextArea.setText(content);

        scriptComboBox.addActionListener(l ->
        {
            Script newScript = scriptComboBox.getItemAt(scriptComboBox.getSelectedIndex());
            String newContent = newScript == null ? "" : newScript.content();

            rSyntaxTextArea.setText(newContent);
        });

        scrollableCodeEditor.setLineNumbersEnabled(true);
        panel.add(scrollableCodeEditor, BorderLayout.CENTER);

        JButton attackButton = getAttackButton(rSyntaxTextArea);

        if (aiManager.isEnabled()) {
            JSplitPane controlSplitPanel = new JSplitPane(
                    VERTICAL_SPLIT,
                    getBurpAIPanel(rSyntaxTextArea),
                    attackButton
            );
            controlSplitPanel.setResizeWeight(1.0);
            controlSplitPanel.setDividerSize(0);
            panel.add(controlSplitPanel, BorderLayout.SOUTH);
        } else {
            panel.add(attackButton, BorderLayout.SOUTH);
        }

        return panel;
    }

    private Component getButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        scriptComboBox = new JComboBox<>(controller.loadScripts());
        buttonPanel.add(scriptComboBox, BorderLayout.CENTER);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton selectScriptsDirectoryButton = getScriptsDirectoryButton();
        east.add(selectScriptsDirectoryButton);

        JLabel threadsLabel = new JLabel("Number of threads:");
        east.add(threadsLabel);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 0, 50, 1);
        numberOfThreadsSpinner = new JSpinner(spinnerModel);
        east.add(numberOfThreadsSpinner);

        buttonPanel.add(east, BorderLayout.EAST);

        return buttonPanel;
    }

    private JButton getScriptsDirectoryButton() {
        JButton selectScriptsDirectoryButton = new JButton("Choose scripts directory");
        selectScriptsDirectoryButton.addActionListener(l ->
        {
            JFileChooser scriptsFileChooser = new JFileChooser();
            scriptsFileChooser.setFileSelectionMode(DIRECTORIES_ONLY);

            int option = scriptsFileChooser.showOpenDialog(this);

            if (option == APPROVE_OPTION) {
                File file = scriptsFileChooser.getSelectedFile();
                Script[] scripts = controller.loadScripts(file);

                scriptComboBox.setModel(new DefaultComboBoxModel<>(scripts));
            }
        });

        return selectScriptsDirectoryButton;
    }

    private Component getBurpAIPanel(JTextComponent scriptTextComponent) {
        JPanel burpAIPanel = new JPanel(new BorderLayout());

        JLabel outputLabel = new JLabel("Burp AI:");
        outputLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        outputLabel.setFont(outputLabel.getFont().deriveFont(Font.BOLD));

        JLabel logLabel = new JLabel("helps you to create Turbo Intruder scripts");
        logLabel.setHorizontalAlignment(SwingConstants.LEFT);


        JButton sendButton = new JButton("Ask AI");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(outputLabel, BorderLayout.WEST);
        topPanel.add(logLabel, BorderLayout.CENTER);
        topPanel.add(sendButton, BorderLayout.EAST);

        RSyntaxTextArea inputArea = rstaFactory.build();
        inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        inputArea.setRows(3);
        inputArea.setColumns(50);
        inputArea.setForeground(Color.GRAY);
        String prompt = "Create a script that will ";
        inputArea.setText(prompt);
        inputArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (inputArea.getText().equals(prompt)) {
                    inputArea.setText("");
                    inputArea.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputArea.getText().trim().isEmpty()) {
                    inputArea.setText(prompt);
                    inputArea.setForeground(Color.GRAY);
                }
            }
        });
        RTextScrollPane scrollableCodeEditor = new RTextScrollPane(inputArea);
//        JTextArea inputArea = new JTextArea(3, 50);
//
//        JScrollPane scrollableCodeEditor = new JScrollPane(inputArea);
        burpAIPanel.add(topPanel, BorderLayout.NORTH);
        burpAIPanel.add(scrollableCodeEditor, BorderLayout.CENTER);

        sendButton.addActionListener(l -> {
            aiManager.prompt(AiUtilities.SYSTEM_PROMPT, sendButton, inputArea, scriptTextComponent);
        });

        return burpAIPanel;
    }

    private JButton getAttackButton(JTextComponent scriptTextComponent) {
        JButton attackButton = new JButton("Attack");
        attackButton.setToolTipText("Ctr + Space or Ctr + Enter to start");
        Utilities.setupFocusableButton(attackButton);
        Action attackAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AttackDetails attackDetails = new AttackDetails(
                        (int) numberOfThreadsSpinner.getValue(),
                        webSocketsMessageEditor.getContents().toString(),
                        upgradeHttpMessageEditor.getRequest(),
                        scriptTextComponent.getText()
                );

                try {
                    controller.startAttack(attackDetails);
                    controller.saveScript(scriptTextComponent.getText());
                } catch (Exception ex) {
                    showMessageDialog(
                            getRootPane(),
                            "Jython code error. Please review.\n" + ex.getMessage(),
                            "Error",
                            ERROR_MESSAGE
                    );
                }
            }
        };
        attackButton.addActionListener(attackAction);

        InputMap inputMap = attackButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = attackButton.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control ENTER"), "toggleAttack");
        inputMap.put(KeyStroke.getKeyStroke("control SPACE"), "toggleAttack");
        actionMap.put("toggleAttack", attackAction);

        return attackButton;
    }
}
