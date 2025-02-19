package turbo;

import burp.api.montoya.core.Registration;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.WebSocketCreatedHandler;
import burp.api.montoya.websocket.WebSockets;
import data.RawWebsocketConfig;
import logger.Logger;

import java.nio.charset.StandardCharsets;

public class RawWebSockets implements WebSockets {
    private final Logger logger;
    private final RawWebsocketConfig config;

    public RawWebSockets(Logger logger, RawWebsocketConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public Registration registerWebSocketCreatedHandler(WebSocketCreatedHandler webSocketCreatedHandler) {
        return null;
    }

    @Override
    public RawExtensionWebSocketCreation createWebSocket(HttpService httpService, String s) {
        try {
            RawExtensionWebSocket ws = new RawExtensionWebSocket(
                    httpService.host(),
                    httpService.port(),
                    httpService.secure(),
                    s.getBytes(StandardCharsets.UTF_8),
                    logger,
                    config
            );
            return new RawExtensionWebSocketCreation(ws);
        } catch (RawException exception) {
            return RawExtensionWebSocketCreation.empty();
        }
    }

    @Override
    public RawExtensionWebSocketCreation createWebSocket(HttpRequest httpRequest) {
        return createWebSocket(httpRequest.httpService(), httpRequest.toString());
    }

}
