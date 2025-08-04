package burp;

import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.WebSocketContextMenuEvent;
import data.HttpContextMenuEventAdapter;
import data.InitialWebSocketMessage;
import data.WebSocketContextMenuEventAdapter;
import ui.EmbeddedServerFrameFactory;
import ui.WebSocketFrameFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static burp.WebSocketFuzzer.EXTENSION_NAME;

public class WebSocketContextMenuItemsProvider implements ContextMenuItemsProvider {
    private final Consumer<JFrame> newFrameConsumer;
    private final WebSocketFrameFactory webSocketFrameFactory;
    private final EmbeddedServerFrameFactory embeddedServerFrameFactory;
    private final HttpContextMenuEventAdapter httpContextMenuEventAdapter;
    private final WebSocketContextMenuEventAdapter webSocketContextMenuEventAdapter;

    public WebSocketContextMenuItemsProvider(
            Consumer<JFrame> newFrameConsumer,
            WebSocketFrameFactory webSocketFrameFactory,
            EmbeddedServerFrameFactory embeddedServerFrameFactory
    ) {
        this.newFrameConsumer = newFrameConsumer;
        this.webSocketFrameFactory = webSocketFrameFactory;
        this.embeddedServerFrameFactory = embeddedServerFrameFactory;
        this.webSocketContextMenuEventAdapter = new WebSocketContextMenuEventAdapter();
        this.httpContextMenuEventAdapter = new HttpContextMenuEventAdapter();
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        if (event.messageEditorRequestResponse().isPresent()) {
            JMenuItem sendToContextMenuItem = new JMenuItem("Send to " + EXTENSION_NAME);
            sendToContextMenuItem.addActionListener(l -> performHttpAction(event));

            return List.of(sendToContextMenuItem);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Component> provideMenuItems(WebSocketContextMenuEvent event) {
        JMenuItem sendToContextMenuItem = new JMenuItem("Send to " + EXTENSION_NAME);
        sendToContextMenuItem.addActionListener(l -> performWebSocketAction(event));

        JMenuItem sendToEmbeddedServerContextMenuItem = new JMenuItem("Send to WebSocket HTTP middleware");
        sendToEmbeddedServerContextMenuItem.addActionListener(l -> performEmbeddedHttpServerAction(event));

        return List.of(sendToContextMenuItem, sendToEmbeddedServerContextMenuItem);
    }

    private void performWebSocketAction(WebSocketContextMenuEvent event) {
        webSocketContextMenuEventAdapter.apply(event).stream()
                .map(webSocketFrameFactory::from)
                .forEach(newFrameConsumer);
    }

    private void performHttpAction(ContextMenuEvent event) {
        List<InitialWebSocketMessage> messages = httpContextMenuEventAdapter.apply(event);
        if (messages.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "No valid HTTP request was found.\nCheck if the request is malformed or missing.",
                    "Unable to Send to " + EXTENSION_NAME,
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        messages.stream()
                .map(webSocketFrameFactory::from)
                .forEach(newFrameConsumer);
    }

    private void performEmbeddedHttpServerAction(WebSocketContextMenuEvent event) {
        newFrameConsumer.accept(embeddedServerFrameFactory.from(webSocketContextMenuEventAdapter.apply(event)));
    }
}
