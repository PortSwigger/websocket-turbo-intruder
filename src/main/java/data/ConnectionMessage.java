package data;

import burp.api.montoya.websocket.Direction;
import connection.Connection;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;

public interface ConnectionMessage {
    String getMessage();

    Direction getDirection();

    long getPayloadLength();

    int getMessageLength();

    LocalDateTime getDateTime();

    long getDateTimeInMilliseconds();

    void setTime(long time);

    long getTime();

    String getComment();

    void setComment(String comment);

    Connection getConnection();

    void send();

    long getId();

    long getHash();

    void setHash(long id);

    int getCode();

    void setAttributes(Map<WebSocketAttributeType, Integer> attributes);

    int getWordCount();

    int getKeyCount();

    boolean isInteresting();

    void setInteresting(boolean interesting);

    ByteBuffer getMessageByteBuffer();
}
