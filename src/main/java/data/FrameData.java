package data;

import java.nio.charset.StandardCharsets;

public class FrameData {
    private final FrameType opcode;
    private final byte[] payload;
    private final long length;

    public FrameData(FrameType type, byte[] payload) {
        this(type, payload, payload.length);
    }

    public FrameData(FrameType type, byte[] payload, long length) {
        this.opcode = type;
        this.payload = payload;
        this.length = length;
    }

    // Static methods for creating frames for each type
    public static FrameData textFrame(String message) {
        return new FrameData(FrameType.TEXT, message.getBytes(StandardCharsets.UTF_8));
    }

    public static FrameData binaryFrame(byte[] data) {
        return new FrameData(FrameType.BINARY, data);
    }

    public static FrameData closeFrame() {
        return new FrameData(FrameType.CLOSE, new byte[]{});
    }

    public static FrameData pingFrame(byte[] payload) {
        return new FrameData(FrameType.PING, payload);
    }

    public static FrameData pingFrame() {
        return new FrameData(FrameType.PING, new byte[]{});
    }

    public static FrameData pongFrame(byte[] payload) {
        return new FrameData(FrameType.PONG, payload);
    }

    public FrameType getOpcode() {
        return opcode;
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "Frame Data {\"opcode\":" + opcode + ", \"payload\":\"" + new String(payload) + "\"}";
    }
}
