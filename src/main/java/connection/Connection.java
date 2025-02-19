package connection;

import burp.api.montoya.http.message.requests.HttpRequest;
import data.ConnectionMessage;

public interface Connection {
    void queue(String payload);

    void queue(int code, byte[] payload);

    void queue(int code, byte[] payload, int length);

    void queue(int code, byte[] payload, String length);

    void queue(String payload, String replacement);

    void queueWithComment(String payload, String comment);

    void queueWithComment(String payload, String replacement, String comment);

    void sendMessage(ConnectionMessage message);

    HttpRequest upgradeRequest();
}
