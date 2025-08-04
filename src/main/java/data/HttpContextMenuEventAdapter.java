package data;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.StatusCodeClass;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class HttpContextMenuEventAdapter implements Function<ContextMenuEvent, List<InitialWebSocketMessage>> {
    private static Optional<InitialWebSocketMessage> convertEventToMessage(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse) {
        try {
            HttpRequestResponse requestResponse = messageEditorHttpRequestResponse.requestResponse();
            HttpRequest request = requestResponse.request();
            String url = request.url();

            if (requestResponse.hasResponse() &&
                    requestResponse.response().isStatusCodeClass(StatusCodeClass.CLASS_1XX_INFORMATIONAL_RESPONSE)) {
                return Optional.of(new InitialWebSocketMessage(request, ByteArray.byteArray("%s")));
            }

            HttpRequest upgradeRequest = request
                    .withHeader("Origin", url)
                    .withHeader("Upgrade", "websocket")
                    .withHeader("Connection", "Upgrade")
                    .withHeader("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                    .withHeader("Sec-WebSocket-Version", "13");

            return Optional.of(new InitialWebSocketMessage(upgradeRequest, ByteArray.byteArray("%s")));
        } catch (MalformedRequestException e) {
            System.err.println("Malformed request encountered: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<InitialWebSocketMessage> apply(ContextMenuEvent event) {
        return event.messageEditorRequestResponse()
                .flatMap(HttpContextMenuEventAdapter::convertEventToMessage)
                .map(List::of)
                .orElseGet(ArrayList::new);
    }

}
