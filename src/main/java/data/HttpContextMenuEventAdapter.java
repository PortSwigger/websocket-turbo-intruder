package data;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.StatusCodeClass;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class HttpContextMenuEventAdapter implements Function<ContextMenuEvent, List<InitialWebSocketMessage>> {
    private static InitialWebSocketMessage convertEventToMessage(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse) {
        HttpRequestResponse requestResponse = messageEditorHttpRequestResponse.requestResponse();

        if (requestResponse.hasResponse() && requestResponse.response().isStatusCodeClass(StatusCodeClass.CLASS_1XX_INFORMATIONAL_RESPONSE)) {
            return new InitialWebSocketMessage(requestResponse.request(), ByteArray.byteArray("%s"));
        } else {
            HttpRequest request = requestResponse.request();
            HttpRequest upgradeRequest = request
                    .withHeader("Origin", request.url())
                    .withHeader("Upgrade", "websocket")
                    .withHeader("Connection", "Upgrade")
                    .withHeader("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                    .withHeader("Sec-WebSocket-Version", "13");
            return new InitialWebSocketMessage(upgradeRequest, ByteArray.byteArray("%s"));
        }
    }

    @Override
    public List<InitialWebSocketMessage> apply(ContextMenuEvent event) {
        return event.messageEditorRequestResponse().isPresent()
                ? List.of(convertEventToMessage(event.messageEditorRequestResponse().get()))
                : new ArrayList<>();

    }

}
