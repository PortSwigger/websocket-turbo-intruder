package data;

import burp.api.montoya.websocket.Direction;
import connection.WebSocketConnection;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;

public class WebSocketConnectionMessage implements ConnectionMessage {
    private final String message;
    private final Direction direction;
    private final int length;
    private final LocalDateTime dateTime;
    private final WebSocketConnection connection;
    private long id;
    private String comment;
    private long time;
    private boolean interesting;
    private Map<WebSocketAttributeType, Integer> attributes;

    public WebSocketConnectionMessage(long id, String message, Direction direction, WebSocketConnection connection) {
        this(id, message, direction, null, connection);
    }

    public WebSocketConnectionMessage(
            long id,
            String message,
            Direction direction,
            String comment,
            WebSocketConnection connection
    ) {
        this.id = id;
        this.message = message;
        this.direction = direction;
        this.length = message.length();
        this.dateTime = now();
        this.comment = comment;
        this.connection = connection;
        this.attributes = new HashMap<>();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public long getPayloadLength() {
        return length;
    }

    @Override
    public int getMessageLength() {
        return length;
    }

    @Override
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public long getDateTimeInMilliseconds() {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public WebSocketConnection getConnection() {
        return connection;
    }

    public void send() {
        connection.sendMessage(this);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getHash() {
        return id;
    }

    @Override
    public void setHash(long id) {
        this.id = id;
    }

    @Override
    public int getCode() {
        return FrameType.TEXT.value;
    }

    @Override
    public void setAttributes(Map<WebSocketAttributeType, Integer> attributes) {
        this.attributes = attributes;
    }

    @Override
    public int getWordCount() {
        return this.attributes.getOrDefault(WebSocketAttributeType.WORD_COUNT, 0);
    }

    @Override
    public int getKeyCount() {
        return this.attributes.getOrDefault(WebSocketAttributeType.KEY_COUNT, 0);
    }

    @Override
    public boolean isInteresting() {
        return interesting;
    }

    @Override
    public void setInteresting(boolean interesting) {
        this.interesting = interesting;
    }

    @Override
    public ByteBuffer getMessageByteBuffer() {
        return ByteBuffer.wrap(message.getBytes());
    }
}
