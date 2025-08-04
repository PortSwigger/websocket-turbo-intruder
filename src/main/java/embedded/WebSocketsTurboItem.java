package embedded;


import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import data.InitialWebSocketMessage;

import java.util.List;
import java.util.Optional;

public class WebSocketsTurboItem implements TurboTableItem {
    private final HttpRequest upgradeRequest;
    private final List<InitialWebSocketMessage> messages;

    public WebSocketsTurboItem(HttpRequest upgradeRequest, List<InitialWebSocketMessage> messages) {
        this.upgradeRequest = upgradeRequest;
        this.messages = messages;
    }

    @Override
    public String getURL() {
        try {
            return upgradeRequest.url();
        } catch (MalformedRequestException e) {
            return "";
        }
    }

    @Override
    public String getComment() {
        return upgradeRequest.bodyToString();
    }

    @Override
    public ByteArray getContent() {
        return upgradeRequest.toByteArray();
    }

    @Override
    public Optional<HttpRequest> getHttpRequest() {
        return Optional.of(upgradeRequest);
    }

    public HttpRequest getUpgradeRequest() {
        return upgradeRequest;
    }

    public List<InitialWebSocketMessage> getMessages() {
        return messages;
    }
}
