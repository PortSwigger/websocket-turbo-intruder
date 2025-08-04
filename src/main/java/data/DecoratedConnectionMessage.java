package data;

import burp.api.montoya.websocket.Direction;
import connection.Connection;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;

public class DecoratedConnectionMessage implements ConnectionMessage {
    private final ConnectionMessage webSocketConnectionMessage;

    public DecoratedConnectionMessage(ConnectionMessage webSocketConnectionMessage) {
        this.webSocketConnectionMessage = webSocketConnectionMessage;
    }

    @Override
    public String getMessage() {
        return webSocketConnectionMessage.getMessage();
    }

    @Override
    public Direction getDirection() {
        return webSocketConnectionMessage.getDirection();
    }

    @Override
    public long getPayloadLength() {
        return webSocketConnectionMessage.getPayloadLength();
    }

    @Override
    public int getMessageLength() {
        return webSocketConnectionMessage.getMessageLength();
    }

    @Override
    public LocalDateTime getDateTime() {
        return webSocketConnectionMessage.getDateTime();
    }

    @Override
    public long getDateTimeInMilliseconds() {
        return webSocketConnectionMessage.getDateTimeInMilliseconds();
    }

    @Override
    public long getTime() {
        return webSocketConnectionMessage.getTime();
    }

    @Override
    public void setTime(long time) {
        webSocketConnectionMessage.setTime(time);
    }

    @Override
    public String getComment() {
        return webSocketConnectionMessage.getComment();
    }

    @Override
    public void setComment(String comment) {
        webSocketConnectionMessage.setComment(comment);
    }

    @Override
    public Connection getConnection() {
        return webSocketConnectionMessage.getConnection();
    }

    @Override
    public void send() {
    }

    @Override
    public long getId() {
        return webSocketConnectionMessage.getId();
    }

    @Override
    public long getHash() {
        return webSocketConnectionMessage.getHash();
    }

    @Override
    public void setHash(long id) {
        webSocketConnectionMessage.setHash(id);
    }

    @Override
    public FrameType getCode() {
        return FrameType.TEXT;
    }

//    @Override
//    public void setAttributes(Map<WebSocketAttributeType, Integer> attributes) {
//        webSocketConnectionMessage.setAttributes(attributes);
//    }

//    @Override
//    public int getWordCount() {
//        return webSocketConnectionMessage.getWordCount();
//    }

//    @Override
//    public int getKeyCount() {
//        return webSocketConnectionMessage.getKeyCount();
//    }

    @Override
    public boolean isInteresting() {
        return webSocketConnectionMessage.isInteresting();
    }

    @Override
    public void setInteresting(boolean interesting) {
        webSocketConnectionMessage.setInteresting(interesting);
    }

    @Override
    public ByteBuffer getMessageByteBuffer() {
        return webSocketConnectionMessage.getMessageByteBuffer();
    }

}
