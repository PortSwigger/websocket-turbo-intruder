package data;

import burp.api.montoya.websocket.Direction;
import connection.Connection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;


public class WSConnectionMessage implements ConnectionMessage {
    private final String message;
    private final Direction direction;
    private final Integer length;
    private final Long timestamp;
    private final Long id;
    private final String comment;

    private final FrameType code;
    private boolean interesting;

    public WSConnectionMessage(ConnectionMessage connectionMessage) {
        this.message = connectionMessage.getMessage();
        this.direction = connectionMessage.getDirection();
        this.length = connectionMessage.getMessageLength();
        this.timestamp = connectionMessage.getDateTimeInMilliseconds();
        this.id = connectionMessage.getId();
        this.comment = connectionMessage.getComment();
        this.code = connectionMessage.getCode();
        this.interesting = connectionMessage.isInteresting();
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
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    @Override
    public long getDateTimeInMilliseconds() {
        return timestamp;
    }

    @Override
    public long getTime() {
        return timestamp;
    }

    @Override
    public void setTime(long time) {}

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {}

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public void send() {}

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
    }

    @Override
    public FrameType getCode() {
        return code;
    }

//    @Override
//    public void setAttributes(Map<WebSocketAttributeType, Integer> attributes) {
//
//    }

//    @Override
//    public int getWordCount() {
//        return 0;
//    }

//    @Override
//    public int getKeyCount() {
//        return 0;
//    }

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
        return ByteBuffer.wrap(message.getBytes(StandardCharsets.ISO_8859_1));
    }
}