package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.EnhancedCapability;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.Ai;
import burp.api.montoya.extension.Extension;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.websocket.WebSockets;
import config.FileLocationConfiguration;
import logger.Logger;
import logger.LoggerLevel;
import socket.TurboWebSocketMessagePublisher;
import ui.EmbeddedServerFrameFactory;
import ui.WebSocketFrameFactory;
import utils.AiUtilities;

import java.util.Set;

import static burp.api.montoya.EnhancedCapability.AI_FEATURES;
import static utils.Utilities.generateMenu;

public class WebSocketFuzzer implements BurpExtension {
    public static final String EXTENSION_NAME = "WebSocket Turbo Intruder";
    private static final String GREETINGS = """
            Usage
            
            Intruder tool
            1. Right-click on a WebSocket message and navigate to Extensions > WebSocket Turbo Intruder > Send to WebSocket Turbo Intruder.
            If you highlight a portion of the message before sending, the highlighted content will be replaced with %s.
            2. Select a template from the drop-down list.
            3. Modify the Python code to fit your use case.
            4. Start the attack.
            
            Middleware tool
            1. Right-click on a WebSocket message and navigate to Extensions > WebSocket Turbo Intruder > Send to Embedded Server.
            2. Click the Start button to launch the HTTP server middleware.
            3. For each unique WebSocket connection, the extension will create an HTTP POST endpoint that routes requests to the remote server.
            4. Select the HTTP server from the table list and send requests through one of Burp Suite's tools.
            """;


    @Override
    public void initialize(MontoyaApi api) {
        Extension extension = api.extension();
        Preferences preferences = api.persistence().preferences();
        UserInterface userInterface = api.userInterface();
        WebSockets websockets = api.websockets();
        Ai ai = AiUtilities.methodExists(api.getClass(), "ai") ? api.ai() : null;
        Logger logger = new Logger(api.logging());
        WebSocketFuzzerFrames frames = new WebSocketFuzzerFrames(logger);
        TurboWebSocketMessagePublisher publisher = new TurboWebSocketMessagePublisher();


        FileLocationConfiguration fileLocationConfiguration =
                new FileLocationConfiguration(logger, preferences);
        WebSocketFrameFactory webSocketFrameFactory =
                new WebSocketFrameFactory(logger, userInterface, fileLocationConfiguration, websockets, ai, publisher);
        EmbeddedServerFrameFactory embeddedServerFrameFactory =
                new EmbeddedServerFrameFactory(logger, userInterface);

        userInterface.menuBar().registerMenu(generateMenu(logger, fileLocationConfiguration, frames::close));

        userInterface.registerContextMenuItemsProvider(
                new WebSocketContextMenuItemsProvider(
                        frames::add,
                        webSocketFrameFactory,
                        embeddedServerFrameFactory
                )
        );

        extension.registerUnloadingHandler(frames::close);

        extension.setName(EXTENSION_NAME);

        String extensionVersion = WebSocketFuzzer.class.getPackage().getImplementationVersion();
        logger.logOutput(LoggerLevel.DEFAULT, EXTENSION_NAME + " v" + extensionVersion);
        logger.logOutput(LoggerLevel.DEFAULT, GREETINGS);
    }

    @Override
    public Set<EnhancedCapability> enhancedCapabilities() {
        return Set.of(AI_FEATURES);
    }
}