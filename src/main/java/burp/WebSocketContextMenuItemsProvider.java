package burp;

import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.WebSocketContextMenuEvent;
import data.HttpContextMenuEventAdapter;
import data.WebSocketContextMenuEventAdapter;
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
    private final HttpContextMenuEventAdapter httpContextMenuEventAdapter;
    private final WebSocketContextMenuEventAdapter webSocketContextMenuEventAdapter;

    public WebSocketContextMenuItemsProvider(
            Consumer<JFrame> newFrameConsumer,
            WebSocketFrameFactory webSocketFrameFactory
    ) {
        this.newFrameConsumer = newFrameConsumer;
        this.webSocketFrameFactory = webSocketFrameFactory;
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

        return List.of(sendToContextMenuItem);
    }

    private void performWebSocketAction(WebSocketContextMenuEvent event) {
        webSocketContextMenuEventAdapter.apply(event).stream()
                .map(webSocketFrameFactory::from)
                .forEach(newFrameConsumer);
    }

    private void performHttpAction(ContextMenuEvent event) {
        httpContextMenuEventAdapter.apply(event).stream()
                .map(webSocketFrameFactory::from)
                .forEach(newFrameConsumer);
    }
}
