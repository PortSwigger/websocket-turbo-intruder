package ui.editor.embedded;

import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.ui.UserInterface;
import config.ExtensionConfiguration;
import embedded.ServerDetails;
import embedded.ServerStarter;
import embedded.TurboTableModel;
import embedded.TurboTablePanel;
import logger.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import script.Script;
import script.ScriptLoaderFacade;
import ui.PanelSwitcher;
import ui.editor.AiManager;
import ui.editor.ThemeAwareRSTAFactory;
import utils.AiUtilities;
import utils.Utilities;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.text.NumberFormat;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;
import static utils.Utilities.setupFocusableButton;
import java.util.UUID;

public class EmbeddedServerEditorPanel extends JPanel {
    private final ThemeAwareRSTAFactory rstaFactory;
    private final EmbeddedServerEditorController controller;
    private final AiManager aiManager;
    private JComboBox<Script> scriptComboBox;
    private JFormattedTextField portTextField;
    private JFormattedTextField timeoutTextField;

    public EmbeddedServerEditorPanel(
            AiManager aiManager,
            ExtensionConfiguration extensionConfiguration,
            Logger logger,
            PanelSwitcher panelSwitcher,
            ServerStarter serverStarter,
            Organizer organizer,
            UserInterface userInterface,
            TurboTableModel tableModel
    ) {
        super(new BorderLayout());

        this.aiManager = aiManager;

        ScriptLoaderFacade scriptLoader = new ScriptLoaderFacade(extensionConfiguration);
        this.rstaFactory = new ThemeAwareRSTAFactory(userInterface, logger);

        controller = new EmbeddedServerEditorController(serverStarter, extensionConfiguration, scriptLoader, panelSwitcher);

        JSplitPane splitPane = new JSplitPane(VERTICAL_SPLIT,
                new TurboTablePanel(organizer, userInterface, tableModel),
                getPythonCodeEditor()
        );
        splitPane.setResizeWeight(0.5);
        this.add(splitPane, BorderLayout.CENTER);
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

        JButton attackButton = getStartButton(rSyntaxTextArea);

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

        east.add(new JLabel("Port:"));
        portTextField = makePortField();
        east.add(portTextField);

        east.add(new JLabel("Timeout:"));
        timeoutTextField = makeTimeoutField();
        east.add(timeoutTextField);

        buttonPanel.add(east, BorderLayout.EAST);

        return buttonPanel;
    }

    private JFormattedTextField makePortField() {
        NumberFormatter nf = new NumberFormatter(NumberFormat.getIntegerInstance());
        nf.setValueClass(Integer.class);
        nf.setMinimum(0);
        nf.setMaximum(65535);
        nf.setAllowsInvalid(false);
        JFormattedTextField f = new JFormattedTextField(nf);
        f.setValue(9000);
        f.setColumns(4);
        f.setToolTipText("Enter a port between 0 and 65535");
        return f;
    }

    private JFormattedTextField makeTimeoutField() {
        NumberFormatter nf = new NumberFormatter(NumberFormat.getIntegerInstance());
        nf.setValueClass(Integer.class);
        nf.setMinimum(0);
        nf.setMaximum(300);
        nf.setAllowsInvalid(false);
        JFormattedTextField f = new JFormattedTextField(nf);
        f.setValue(5);
        f.setColumns(3);
        f.setToolTipText("Enter a timeout in seconds between 0 and 300");
        return f;
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
        burpAIPanel.add(topPanel, BorderLayout.NORTH);
        burpAIPanel.add(scrollableCodeEditor, BorderLayout.CENTER);

        sendButton.addActionListener(l -> {
            aiManager.prompt(AiUtilities.SYSTEM_PROMPT_MIDDLEWARE, sendButton, inputArea, scriptTextComponent);
        });

        return burpAIPanel;
    }

    private JButton getStartButton(JTextComponent scriptTextComponent) {
        JButton startButton = new JButton("Start");
        startButton.setToolTipText("Ctr + Space or Ctr + Enter to start");
        setupFocusableButton(startButton);

        InputMap inputMap = startButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = startButton.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control ENTER"), "triggerStart");
        inputMap.put(KeyStroke.getKeyStroke("control SPACE"), "triggerStart");

        actionMap.put("triggerStart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.doClick();
            }
        });

        startButton.addActionListener(l -> {
            startButton.setEnabled(false);
            startButton.setText("Connecting…");

            new SwingWorker<Void, Void>() {
                Exception error = null;

                @Override
                protected Void doInBackground() {
                    try {
                        Number portNum = (Number) portTextField.getValue();
                        if (portNum == null)
                            throw new IllegalArgumentException("Please provide a port number");
                        int port = portNum.intValue();
                        if (!Utilities.isPortAvailable(port))
                            throw new IllegalArgumentException(
                                    String.format("Port %d isn’t available", port));

                        Number timeoutNum = (Number) timeoutTextField.getValue();
                        if (timeoutNum == null)
                            throw new IllegalArgumentException("Please provide a timeout in seconds");
                        int timeout = timeoutNum.intValue();

                        ServerDetails sd = new ServerDetails(
                                port,
                                timeout,
                                scriptTextComponent.getText(),
                                UUID.randomUUID().toString());
                        controller.startServer(sd);
                        controller.saveScript(scriptTextComponent.getText());
                    } catch (Exception ex) {
                        error = ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    startButton.setText("Start");
                    startButton.setEnabled(true);

                    if (error instanceof IllegalArgumentException) {
                        JOptionPane.showMessageDialog(
                                SwingUtilities.getWindowAncestor(startButton),
                                "Check your server preferences:\n" + error.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    } else if (error != null) {
                        JOptionPane.showMessageDialog(
                                SwingUtilities.getWindowAncestor(startButton),
                                "Jython code error. Please review.\n" + error.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }.execute();
        });

        return startButton;
    }

}
