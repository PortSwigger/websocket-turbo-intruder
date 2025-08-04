package ui.editor.embedded;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.ui.UserInterface;
import embedded.*;
import ui.PanelSwitcher;
import utils.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;

public class EmbeddedServerStreamPanel extends JPanel {
    private final Organizer organizer;
    private final UserInterface userInterface;
    private final ServerManager serverManager;
    private final ServerStatus serverStatus;
    private final PanelSwitcher panelSwitcher;
    private final WrappedConnectionTableModel wrappedConnectionTableModel;
    private final IconFactory iconFactory;
    private TurboTableItemList requests;

    public EmbeddedServerStreamPanel(
            Organizer organizer,
            UserInterface userInterface,
            ServerManager serverManager,
            ServerStatus serverStatus,
            PanelSwitcher panelSwitcher,
            WrappedConnectionTableModel wrappedConnectionTableModel
    ) {
        super(new BorderLayout());
        this.organizer = organizer;

        this.userInterface = userInterface;
        this.serverManager = serverManager;
        this.serverStatus = serverStatus;
        this.panelSwitcher = panelSwitcher;
        this.wrappedConnectionTableModel = wrappedConnectionTableModel;
        this.iconFactory = new IconFactory(userInterface);

        initComponents();
    }

    private void initComponents() {
        Consumer<ConnectionWrapper> selectedConnectionConsumer = this::updateRequest;
        requests = new TurboTableItemList();
        JSplitPane streamPanelSplitPane = new JSplitPane(
                HORIZONTAL_SPLIT,
                new WrappedConnectionTable(userInterface, wrappedConnectionTableModel, selectedConnectionConsumer),
                new TurboTablePanel(organizer, userInterface, new TurboTableModel(requests)));

        streamPanelSplitPane.setResizeWeight(0.5);

        this.add(streamPanelSplitPane, CENTER);
        this.add(getStopButton(), SOUTH);
    }

    private void updateRequest(ConnectionWrapper connection) {
        requests.clear();
        String uri = serverManager.uri();
        if (connection != null && !uri.isEmpty()) {
            for (ByteArray message : connection.getMessages()) {
                HttpRequest request = HttpRequest.httpRequestFromUrl(uri)
                        .withMethod("POST")
                        .withAddedParameters(
                                HttpParameter.urlParameter("url", URLEncoder.encode(connection.getURL(), Charset.defaultCharset())))
                        .withAddedHeader("Authorization", serverManager.secret())
                        .withBody(message);
                requests.addItem(new HttpRequestTurboItem(request));
            }
        }
    }

    private Component getStopButton() {
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(l -> {
            if (serverStatus.isRunning()) {
                serverManager.stopServer();
                wrappedConnectionTableModel.clear();
                requests.clear();
                panelSwitcher.showEditorPanel();
            }
        });

        return stopButton;
    }
}
