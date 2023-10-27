package ui.editor;

import attack.AttackHandler;
import burp.WebSocketFuzzer;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.ui.Theme;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.contextmenu.WebSocketMessage;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import org.apache.commons.io.IOUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class WebSocketEditorPanel extends JPanel
{
    private final Logging logging;
    private final UserInterface userInterface;
    private final Persistence persistence;
    private final CardLayout cardLayout;
    private final JPanel cardDeck;
    private final AttackHandler attackHandler;
    private final WebSocketMessage webSocketMessage;
    private JComboBox<Path> scriptComboBox;
    private WebSocketMessageEditor webSocketsMessageEditor;

    public WebSocketEditorPanel(
            Logging logging,
            UserInterface userInterface,
            Persistence persistence,
            CardLayout cardLayout,
            JPanel cardDeck,
            AttackHandler attackHandler,
            WebSocketMessage webSocketMessage
    )
    {
        this.logging = logging;
        this.userInterface = userInterface;
        this.persistence = persistence;
        this.cardLayout = cardLayout;
        this.cardDeck = cardDeck;
        this.attackHandler = attackHandler;
        this.webSocketMessage = webSocketMessage;

        this.setLayout(new BorderLayout());

        initComponents();
    }

    private void initComponents()
    {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, getWebSocketMessageEditor(), getPythonCodeEditor());
        splitPane.setResizeWeight(0.3);

        this.add(splitPane, BorderLayout.CENTER);
    }

    private Component getWebSocketMessageEditor()
    {
        webSocketsMessageEditor = userInterface.createWebSocketMessageEditor();
        webSocketsMessageEditor.setContents(webSocketMessage.payload());

        return webSocketsMessageEditor.uiComponent();
    }

    private Component getPythonCodeEditor()
    {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(getButtonPanel(), BorderLayout.NORTH);

        RSyntaxTextArea rSyntaxTextArea = getRSyntaxTextArea();
        RTextScrollPane scrollableCodeEditor = new RTextScrollPane(rSyntaxTextArea);

        scriptComboBox.addActionListener(l -> {
            Path path = scriptComboBox.getItemAt(scriptComboBox.getSelectedIndex());

            if (!path.toString().contains(".py"))
            {
                rSyntaxTextArea.setText(null);
            }
            else if (path.toString().startsWith(WebSocketFuzzer.DEFAULT_SCRIPT_DIRECTORY))
            {
                String data = null;

                try (InputStream stream = WebSocketEditorPanel.class.getResourceAsStream(path.toString()))
                {
                    if (stream != null)
                    {
                        data = IOUtils.toString(stream);
                    }
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                rSyntaxTextArea.setText(data);
            }
            else
            {
                String content;
                try
                {
                    content = Files.readString(path);
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                rSyntaxTextArea.setText(content);
            }
        });

        scrollableCodeEditor.setLineNumbersEnabled(true);
        panel.add(scrollableCodeEditor, BorderLayout.CENTER);

        JButton attackButton = getAttackButton(rSyntaxTextArea);

        panel.add(attackButton, BorderLayout.SOUTH);

        return panel;
    }

    private Component getButtonPanel()
    {
        JPanel buttonPanel = new JPanel();

        scriptComboBox = getScriptComboBox();

        buttonPanel.add(scriptComboBox);

        JButton selectScriptsDirectoryButton = getScriptsDirectoryButton();
        buttonPanel.add(selectScriptsDirectoryButton);

        return buttonPanel;
    }

    private JComboBox<Path> getScriptComboBox()
    {
        List<Path> pathList = getPathList();

        return new JComboBox<>(pathList.toArray(Path[]::new));
    }

    private List<Path> getPathList()
    {
        String websocketScriptsPath = persistence.preferences().getString("websocketsScriptsPath");
        List<Path> pathList = new ArrayList<>();

        if (WebSocketFuzzer.DEFAULT_SCRIPT_DIRECTORY.equals(websocketScriptsPath))
        {
            URL url = WebSocketEditorPanel.class.getResource(websocketScriptsPath);
            if (url != null)
            {
                Stream<Path> stream  = null;
                try
                {
                    URI uri = url.toURI();

                    try (FileSystem fs = FileSystems.newFileSystem(uri, new HashMap<>())) {
                        stream = Files.walk(Paths.get(uri));

                        stream.forEach(pathList::add);
                    }
                } catch (IOException | URISyntaxException e)
                {
                    throw new RuntimeException(e);
                } finally
                {
                    if (stream != null)
                    {
                        stream.close();
                    }
                }
            }
        }
        else
        {
            try (Stream<Path> stream = Files.walk(Paths.get(websocketScriptsPath)))
            {
                stream.forEach(pathList::add);
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return pathList;
    }

    private JButton getScriptsDirectoryButton()
    {
        JButton selectScriptsDirectoryButton = new JButton("Choose scripts directory");
        selectScriptsDirectoryButton.addActionListener(l -> {
            JFileChooser scriptsFileChooser = new JFileChooser();
            scriptsFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int option = scriptsFileChooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION)
            {
                File file = scriptsFileChooser.getSelectedFile();
                persistence.preferences().setString("websocketsScriptsPath", file.getAbsolutePath());

                int originalSize = scriptComboBox.getItemCount();

                List<Path> pathList = getPathList();

                for (Path path : pathList)
                {
                    scriptComboBox.addItem(path);
                }

                for (int i=0; i < originalSize; i++)
                {
                    scriptComboBox.removeItemAt(0);
                }
            }
        });

        return selectScriptsDirectoryButton;
    }

    private RSyntaxTextArea getRSyntaxTextArea()
    {
        javax.swing.text.JTextComponent.removeKeymap("RTextAreaKeymap");
        UIManager.put("RTextAreaUI.inputMap", null);
        UIManager.put("RTextAreaUI.actionMap", null);
        UIManager.put("RSyntaxTextAreaUI.inputMap", null);
        UIManager.put("RSyntaxTextAreaUI.actionMap", null);

        RSyntaxTextArea codeEditor = new RSyntaxTextArea(20, 60);

        codeEditor.setEditable(true);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        codeEditor.setAntiAliasingEnabled(true);
        codeEditor.setAutoIndentEnabled(true);
        codeEditor.setPaintTabLines(true);
        codeEditor.setTabSize(4);
        codeEditor.setTabsEmulated(true);
        codeEditor.setEOLMarkersVisible(false);
        codeEditor.setWhitespaceVisible(false);

        if (userInterface.currentTheme() == Theme.DARK)
        {
            try
            {
                org.fife.ui.rsyntaxtextarea.Theme rSyntaxTextAreaTheme = org.fife.ui.rsyntaxtextarea.Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
                rSyntaxTextAreaTheme.apply(codeEditor);
            }
            catch (IOException e)
            {
                logging.logToError("Unable to apply dark theme.");
            }
        }

        return codeEditor;
    }

    private JButton getAttackButton(RSyntaxTextArea rSyntaxTextArea)
    {
        JButton attackButton = new JButton("Attack");
        attackButton.addActionListener(l -> {
            String payload = webSocketsMessageEditor.getContents().toString();

            String jythonCode = rSyntaxTextArea.getText();

            new Thread(() -> {
                try
                {
                    attackHandler.executeJython(payload, jythonCode);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(this, "Jython code error. Please review.\r\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
                    logging.logToError("Jython code error. Please review.\r\n" + e);
                }
            }).start();

            SwingUtilities.invokeLater(() -> cardLayout.show(cardDeck, "attackPanel"));
        });
        return attackButton;
    }
}
