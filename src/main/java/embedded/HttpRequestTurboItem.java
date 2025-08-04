package embedded;


import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;

import java.util.Optional;

public class HttpRequestTurboItem implements TurboTableItem {
    private final HttpRequest request;

    public HttpRequestTurboItem(HttpRequest request) {
        this.request = request;
    }

    @Override
    public String getURL() {
        try {
            return request.url();
        } catch (MalformedRequestException e) {
            return "";
        }
    }

    @Override
    public String getComment() {
        return request.bodyToString();
    }

    @Override
    public ByteArray getContent() {
        return request.toByteArray();
    }

    @Override
    public Optional<HttpRequest> getHttpRequest() {
        return Optional.of(request);
    }
}
