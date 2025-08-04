package ui.editor.embedded;

import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.ui.UserInterface;
import config.ExtensionConfiguration;
import embedded.ServerManager;
import embedded.ServerStatus;
import embedded.TurboTableModel;
import embedded.WrappedConnectionTableModel;
import logger.Logger;
import ui.PanelSwitcher;
import ui.editor.AiManager;

import javax.swing.*;
import java.awt.*;

import static burp.WebSocketFuzzer.EXTENSION_NAME;
import static javax.swing.SwingUtilities.invokeLater;

public class EmbeddedServerFrame extends JFrame {
    private static final String FRAME_NAME = "Embedded Server";
    private static final String SERVER_PANEL_NAME = "streamPanel";
    private static final String EDITOR_PANEL_NAME = "editorPanel";
    private static final double WIDTH_RATIO = 0.5;
    private static final double HEIGHT_RATIO = 0.65;

    public EmbeddedServerFrame(
            Logger logger,
            Organizer organizer,
            UserInterface userInterface,
            AiManager aiManager,
            ExtensionConfiguration extensionConfiguration,
            ServerManager serverManager,
            ServerStatus serverStatus,
            TurboTableModel tableModel,
            WrappedConnectionTableModel wrappedConnectionTableModel) {
        String titleString = EXTENSION_NAME + " - " + FRAME_NAME;

        setTitle(titleString);

        Frame burpFrame = userInterface.swingUtils().suiteFrame();

        Dimension dimension = burpFrame.getSize();

        int width = (int) (dimension.width * WIDTH_RATIO);
        int height = (int) (dimension.height * HEIGHT_RATIO);

        setPreferredSize(new Dimension(width, height));

        CardLayout cardLayout = new CardLayout();
        JPanel cardDeck = new JPanel(cardLayout);

        PanelSwitcher panelSwitcher = new PanelSwitcher() {
            @Override
            public void showAttackPanel() {
                showPanel(SERVER_PANEL_NAME);
            }

            @Override
            public void showEditorPanel() {
                showPanel(EDITOR_PANEL_NAME);
            }

            private void showPanel(String panelName) {
                invokeLater(() -> cardLayout.show(cardDeck, panelName));
            }
        };

        cardDeck.add(
                new EmbeddedServerEditorPanel(
                        aiManager,
                        extensionConfiguration,
                        logger,
                        panelSwitcher,
                        serverManager,
                        organizer,
                        userInterface,
                        tableModel
                ),
                EDITOR_PANEL_NAME
        );

        cardDeck.add(
                new EmbeddedServerStreamPanel(
                        organizer,
                        userInterface,
                        serverManager,
                        serverStatus,
                        panelSwitcher,
                        wrappedConnectionTableModel
                ),
                SERVER_PANEL_NAME
        );

        this.getContentPane().add(cardDeck);
        this.pack();
        this.toFront();
        this.setLocationRelativeTo(burpFrame);
        this.setVisible(true);
    }
}
