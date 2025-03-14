package ui.embedded;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import embedded.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;

import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;

public class EmbeddedServerPanel extends JPanel {
    private final UserInterface userInterface;
    private final ServerManager serverManager;
    private final EmbeddedServerStatus serverStatus;
    private final WrappedConnectionTableModel tableModel;
    private final JTabbedPane requestTabs;

    public EmbeddedServerPanel(
            UserInterface userInterface,
            ServerManager serverManager,
            EmbeddedServerStatus serverStatus,
            WrappedConnectionTableModel tableModel) {

        super(new BorderLayout());

        this.userInterface = userInterface;
        this.serverManager = serverManager;
        this.serverStatus = serverStatus;
        this.tableModel = tableModel;
        this.requestTabs = new JTabbedPane();

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

        requestTabs.setUI(new CustomTabbedPaneUI());

        Consumer<ConnectionWrapper> selectedConnectionConsumer = (connection) -> {
            updateRequestTabs(connection); // Update tabs when a new connection is selected
        };
        JSplitPane attackPanelSplitPane = new JSplitPane(
                HORIZONTAL_SPLIT,
                new WrappedConnectionTable(userInterface, tableModel, selectedConnectionConsumer),
                requestTabs
        );
        attackPanelSplitPane.setResizeWeight(0.5);

        JSplitPane attackPanel = new JSplitPane(
                VERTICAL_SPLIT,
                attackPanelSplitPane,
                getHaltConfigureButton()
        );
        attackPanel.setResizeWeight(1.0);
        attackPanel.setDividerSize(0);

        this.add(attackPanel);
    }

    private void updateRequestTabs(ConnectionWrapper connection) {
        requestTabs.removeAll(); // Clear previous tabs

        if (connection != null) {
            List<ByteArray> messages = connection.getMessages();
            int i = 0;
            for (ByteArray message : messages) {
                HttpRequestEditor requestEditor = userInterface.createHttpRequestEditor(READ_ONLY);
                requestEditor.setRequest(
                        HttpRequest.httpRequestFromUrl("http://127.0.0.1:9000/proxy")
                                .withMethod("POST")
                                .withAddedParameters(
                                        HttpParameter.urlParameter("url", URLEncoder.encode(connection.getURL(), Charset.defaultCharset())))
                                .withBody(message)
                );

//                JPanel editorPanel = createLabeledComponent("Request", requestEditor.uiComponent());
                JPanel editorPanel = new JPanel(new BorderLayout());
                editorPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);
                requestTabs.addTab(String.valueOf(i++), editorPanel);

            }
        }

        requestTabs.revalidate();
        requestTabs.repaint();
    }

    private void clean() {
        tableModel.clear();
        requestTabs.removeAll();
    }

    private Component getHaltConfigureButton() {
        JButton haltConfigureButton = new JButton("Start");
        haltConfigureButton.addActionListener(l -> {
            if (serverStatus.isRunning()) {
                serverManager.stopServer();
                clean();
                haltConfigureButton.setText("Start");
            } else {
                serverManager.startServer();
                haltConfigureButton.setText("Stop");
                // TODO close window
            }
        });

        return haltConfigureButton;
    }

    class CustomTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
            return 20; // Custom tab height
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isSelected) {
                g2.setColor(new Color(255, 100, 50)); // Burp Suite Orange
                g2.setStroke(new BasicStroke(2)); // Thickness of the underline
                g2.drawLine(x + 10, y + h - 2, x + w - 10, y + h - 2); // Bottom underline only
            }
        }

    }

}
