package embedded;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.Optional;

public interface TurboTableItem {
    String getURL();

    String getComment();

    ByteArray getContent();

    Optional<HttpRequest> getHttpRequest();
}
