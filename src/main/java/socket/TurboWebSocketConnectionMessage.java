package socket;

import burp.api.montoya.websocket.Direction;
import connection.Connection;
import data.ConnectionMessage;
import data.FrameType;
import data.WebSocketAttributeType;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;

public class TurboWebSocketConnectionMessage implements ConnectionMessage {
    private final ByteBuffer message;
    private final Direction direction;
    private final LocalDateTime dateTime;
    private final Connection connection;
    private final long length;
    private long id;
    private long hash;
    private int code;
    private String comment;
    private long time;
    private boolean interesting;
    private Map<WebSocketAttributeType, Integer> attributes;

    public TurboWebSocketConnectionMessage(long id, String message, Direction direction, Connection connection) {
        this(id, message, direction, null, connection);
    }

    public TurboWebSocketConnectionMessage(
            long id,
            String message,
            Direction direction,
            String comment,
            Connection connection
    ) {
        this(id, message, direction, comment, now(), connection);
    }

    public TurboWebSocketConnectionMessage(long id, String message, Direction direction, String comment, LocalDateTime dateTime, Connection connection) {
        this(id, FrameType.TEXT.value, ByteBuffer.wrap(message.getBytes(Charset.defaultCharset())), direction, comment, dateTime, connection);
    }

    public TurboWebSocketConnectionMessage(long id, int code, ByteBuffer message, Direction direction, String comment, LocalDateTime dateTime, Connection connection) {
        this(id, code, message, direction, comment, dateTime, connection, message.array().length);
    }

    // TODO calculate real time
    public TurboWebSocketConnectionMessage(long id, int code, ByteBuffer message, Direction direction, String comment, LocalDateTime dateTime, Connection connection, long length) {
        this.hash = id;
        this.id = id;
        this.code = code;
        this.message = message;
        this.direction = direction;
        this.comment = comment;
        this.length = length;
        this.dateTime = dateTime;
        this.connection = connection;
        this.attributes = new HashMap<>();
    }

    @Override
    public String getMessage() {
        return new String(message.array(), StandardCharsets.ISO_8859_1);
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
        return message.array().length;
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
    public Connection getConnection() {
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
        return hash;
    }

    @Override
    public void setHash(long id) {
        this.hash = id;
    }

    @Override
    public int getCode() {
        return code;
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
        return message;
    }
}
