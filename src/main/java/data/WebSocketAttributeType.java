package data;

public enum WebSocketAttributeType {
    CONTENT_LENGTH("Content Length"),
    KEY_NAMES("Key Names"),
    KEY_COUNT("Key Count"),
    SPACE_COUNT("Space Count"),
    LINE_COUNT("Line Count"),
    TIME("Time");

    private final String displayName;

    WebSocketAttributeType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
