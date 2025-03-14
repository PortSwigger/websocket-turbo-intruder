package data;

public enum FrameType {
    CONTINUATION(0x0), TEXT(0x1), BINARY(0x2), CLOSE(0x8), PING(0x9), PONG(0xA);

    public final int value;

    FrameType(int value) {
        this.value = value;
    }

    public static FrameType fromByte(byte b) {
        for (FrameType type : FrameType.values()) {
            if (type.value == (b & 0x0F)) {
                return type;
            }
        }
        return null;
    }

    public static FrameType from(int b) {
        for (FrameType type : FrameType.values()) {
            if (type.value == b) {
                return type;
            }
        }
        return null;
    }
}
