package socket;

import burp.api.montoya.websocket.Direction;
import burp.api.montoya.websocket.TextMessage;

import java.nio.charset.Charset;

public class TurboTextMessage implements TextMessage {
    private final byte[] payload;
    private final Direction direction;

    public TurboTextMessage(byte[] payload, Direction direction) {
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
