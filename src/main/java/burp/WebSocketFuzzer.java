package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.EnhancedCapability;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.Ai;
import burp.api.montoya.core.Registration;
import burp.api.montoya.extension.Extension;
import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.websocket.WebSockets;
import config.ExtensionConfiguration;
import logger.Logger;
import logger.LoggerLevel;
import socket.TurboConnectionPublisher;
import ui.EmbeddedServerFrameFactory;
import ui.WebSocketFrameFactory;
import ui.attack.WebSocketLoggerPanel;
import ui.attack.table.WebSocketMessageTableModel;
import utils.AiUtilities;
import utils.Utilities;

import java.util.Objects;
import java.util.Set;

import static burp.api.montoya.EnhancedCapability.AI_FEATURES;
import static utils.Utilities.generateMenu;

public class WebSocketFuzzer implements BurpExtension {
    public static final String EXTENSION_NAME = "WebSocket Turbo Intruder";
    private static final String EXTENSION_TAB = "WS Logger";

    private Registration wsTabRegistration;

    @Override
    public void initialize(MontoyaApi api) {
        Extension extension = api.extension();
        Preferences preferences = api.persistence().preferences();
        UserInterface userInterface = api.userInterface();
        WebSockets websockets = api.websockets();
        Ai ai = AiUtilities.methodExists(api.getClass(), "ai") ? api.ai() : null;
        Logger logger = new Logger(api.logging());
        Organizer organizer = api.organizer();
        WebSocketFuzzerFrames frames = new WebSocketFuzzerFrames(logger);
        TurboConnectionPublisher publisher = new TurboConnectionPublisher();
        WebSocketMessageTableModel historyTableModel = new WebSocketMessageTableModel(true);

        ExtensionConfiguration extensionConfiguration =
                new ExtensionConfiguration(logger, preferences);
        WebSocketFrameFactory webSocketFrameFactory =
                new WebSocketFrameFactory(logger, userInterface, extensionConfiguration, websockets, ai, publisher, historyTableModel);
        EmbeddedServerFrameFactory embeddedServerFrameFactory =
                new EmbeddedServerFrameFactory(ai, logger, extensionConfiguration, organizer, userInterface, websockets, historyTableModel);

        Boolean loggerOn = Objects.requireNonNullElse(extensionConfiguration.getWebSocketsLogger(), Boolean.FALSE);
        WebSocketLoggerPanel loggerPanel = new WebSocketLoggerPanel(userInterface, historyTableModel);
        if (loggerOn) {
            wsTabRegistration = userInterface.registerSuiteTab(EXTENSION_TAB, loggerPanel);
        }
        Runnable loggerAction = () -> {
            boolean nowOn = !isTabVisible();
            extensionConfiguration.setWebSocketsLogger(nowOn);

            if (nowOn) {
                if (!isTabVisible()) {
                    wsTabRegistration = userInterface.registerSuiteTab(
                            EXTENSION_TAB, loggerPanel
                    );
                }
            } else {
                if (isTabVisible()) {
                    wsTabRegistration.deregister();
                }
            }
        };
        userInterface.menuBar().registerMenu(generateMenu(logger, extensionConfiguration, loggerAction, frames::close));

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
        logger.logOutput(LoggerLevel.DEFAULT, Utilities.GREETINGS);
    }

    private boolean isTabVisible() {
        return wsTabRegistration != null && wsTabRegistration.isRegistered();
    }

    @Override
    public Set<EnhancedCapability> enhancedCapabilities() {
        return Set.of(AI_FEATURES);
    }
}