package socket;

import burp.api.montoya.core.Registration;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.WebSocketCreatedHandler;
import burp.api.montoya.websocket.WebSockets;
import logger.Logger;
import logger.LoggerLevel;
import turbo.RawException;

import java.nio.charset.StandardCharsets;

public class TurboWebSockets implements WebSockets {
    private final Logger logger;
    private final TurboConfig config;

    public TurboWebSockets(Logger logger, TurboConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public Registration registerWebSocketCreatedHandler(WebSocketCreatedHandler webSocketCreatedHandler) {
        return null;
    }

    @Override
    public TurboExtensionWebSocketCreation createWebSocket(HttpService httpService, String s) {
//        try {
//            TurboExtensionWebSocket ws = new TurboExtensionWebSocket(
//                    httpService.host(),
//                    httpService.port(),
//                    httpService.secure(),
//                    s.getBytes(StandardCharsets.UTF_8),
//                    logger,
//                    config
//            );
//            if (ws.isOpen()) return new TurboExtensionWebSocketCreation(ws);
//        } catch (RawException exception) {
//            logger.logOutput(LoggerLevel.UI, exception.getMessage());
//        }
//        return TurboExtensionWebSocketCreation.empty();
        return null;
    }

    @Override
    public TurboExtensionWebSocketCreation createWebSocket(HttpRequest httpRequest) {
//        return createWebSocket(httpRequest.httpService(), httpRequest.toString());
        try {
            TurboExtensionWebSocket ws = new TurboExtensionWebSocket(
                    httpRequest.httpService().host(),
                    httpRequest.httpService().port(),
                    httpRequest.httpService().secure(),
                    httpRequest.toString().getBytes(StandardCharsets.UTF_8),
//                    httpRequest.toByteArray().getBytes(),
                    logger,
                    config
            );
            if (ws.isOpen()) return new TurboExtensionWebSocketCreation(ws);
        } catch (RawException exception) {
            logger.logOutput(LoggerLevel.UI, exception.getMessage());
        }
        return TurboExtensionWebSocketCreation.empty();
    }

}
