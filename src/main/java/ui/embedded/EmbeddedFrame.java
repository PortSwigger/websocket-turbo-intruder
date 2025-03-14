package ui.embedded;

import burp.api.montoya.ui.UserInterface;
import embedded.EmbeddedServerState;
import embedded.ServerManager;
import embedded.WrappedConnectionTableModel;
import logger.Logger;

import javax.swing.*;
import java.awt.*;

import static burp.WebSocketFuzzer.EXTENSION_NAME;

public class EmbeddedFrame extends JFrame {
    private static final String FRAME_NAME = "Embedded Server";
    private static final double WIDTH_RATIO = 0.5;
    private static final double HEIGHT_RATIO = 0.65;

    public EmbeddedFrame(
            Logger logger,
            UserInterface userInterface,
            WrappedConnectionTableModel webSocketMessageTableModel,
            ServerManager serverManager,
            EmbeddedServerState serverState) {


        String titleString = EXTENSION_NAME + " - " + FRAME_NAME;

        setTitle(titleString);

        Frame burpFrame = userInterface.swingUtils().suiteFrame();

        Dimension dimension = burpFrame.getSize();

        int width = (int) (dimension.width * WIDTH_RATIO);
        int height = (int) (dimension.height * HEIGHT_RATIO);

        setPreferredSize(new Dimension(width, height));

        JPanel embeddedServerPanel = new EmbeddedServerPanel(
                userInterface,
                serverManager,
                serverState,
                webSocketMessageTableModel
        );

        this.getContentPane().add(embeddedServerPanel);

        this.pack();
        this.toFront();
        this.setLocationRelativeTo(burpFrame);
        this.setVisible(true);
    }


}
