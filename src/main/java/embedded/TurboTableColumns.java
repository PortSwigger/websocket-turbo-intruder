package embedded;

import static java.util.Arrays.stream;

public enum TurboTableColumns {
    MESSAGE_ID("ID", 10, Integer.class),
    URI("URI", 40, String.class),
    COMMENT("Comment", 50, String.class);

    private final String header;
    private final int widthPercentage;
    private final Class<?> type;

    TurboTableColumns(String header, int widthPercentage, Class<?> type) {
        this.header = header;
        this.widthPercentage = widthPercentage;
        this.type = type;
    }

    static int[] columnWidthPercentages() {
        return stream(values()).mapToInt(c -> c.widthPercentage).toArray();
    }

    static String headerWithIndex(int index) {
        return values()[index].header;
    }

    static Class<?> typeForIndex(int index) {
        return values()[index].type;
    }
}
