package turbo;

import burp.api.montoya.websocket.Direction;
import burp.api.montoya.websocket.TextMessage;

import java.nio.charset.Charset;

public class RawTextMessage implements TextMessage {
    private final byte[] payload;
    private final Direction direction;

    public RawTextMessage(byte[] payload, Direction direction) {
        this.payload = payload;
        this.direction = direction;
    }

    @Override
    public String payload() {
        return new String(payload, Charset.defaultCharset());
    }

    @Override
    public Direction direction() {
        return direction;
    }
}
