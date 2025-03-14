package socket;

import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;

public interface TurboWebSocketInterface {
    public boolean isOpen();

    public ExtensionWebSocketCreationStatus status();

    void stop();
}
