package turbo;

import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.websocket.extension.ExtensionWebSocket;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreation;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;

import java.util.Optional;

public class RawExtensionWebSocketCreation implements ExtensionWebSocketCreation {
    private final ExtensionWebSocketCreationStatus status;
    private final ExtensionWebSocket websocket;
    private final HttpResponse response;

    private RawExtensionWebSocketCreation() {
        this.websocket = null;
        this.response = null;
        this.status = ExtensionWebSocketCreationStatus.CONNECTION_FAILED;
    }

    public RawExtensionWebSocketCreation(RawExtensionWebSocket websocket) {
        if (websocket == null) {
            throw new IllegalArgumentException("WebSocket cannot be null");
        }
        this.websocket = websocket;
        this.response = websocket.getResponse();
        this.status = websocket.getStatus();
    }

    public static RawExtensionWebSocketCreation empty() {
        return new RawExtensionWebSocketCreation();
    }

    @Override
    public ExtensionWebSocketCreationStatus status() {
        return status;
    }

    @Override
    public Optional<ExtensionWebSocket> webSocket() {
        return Optional.ofNullable(websocket);
    }

    @Override
    public Optional<HttpResponse> upgradeResponse() {
        return Optional.ofNullable(response);
    }
}
