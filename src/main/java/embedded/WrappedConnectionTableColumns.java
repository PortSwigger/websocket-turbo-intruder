package embedded;

import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;

import static java.util.Arrays.stream;

public enum WrappedConnectionTableColumns {
    MESSAGE_ID("ID", 10, Integer.class),
    REAL_ID("URL", 50, String.class),
    DIRECTION("Status", 40, ExtensionWebSocketCreationStatus.class);

    private final String header;
    private final int widthPercentage;
    private final Class<?> type;

    WrappedConnectionTableColumns(String header, int widthPercentage, Class<?> type) {
        this.header = header;
        this.widthPercentage = widthPercentage;
        this.type = type;
    }

    static int[] columnWidthPercentages() {
        int[] result = stream(values()).mapToInt(c -> c.widthPercentage).toArray();
        return result;
    }

    static String headerWithIndex(int index) {
        return values()[index].header;
    }

    static Class<?> typeForIndex(int index) {
        return values()[index].type;
    }
}
