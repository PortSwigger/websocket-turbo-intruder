package logger;

import burp.api.montoya.logging.Logging;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import static logger.LoggerLevel.DEBUG;

public class Logger {
    private static final int BYTES_PER_LINE = 16;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Logging logging;
    private Consumer<String> fuzzerLogger;
    private boolean debugLogLevel;

    public Logger(Logging logging) {
        this.logging = logging;
        this.debugLogLevel = false;
    }

    public void registerFuzzerLogger(Consumer<String> listener) {
        this.fuzzerLogger = listener;
    }

    public void unregisterFuzzerLogger() {
        this.fuzzerLogger = null;
    }

    public boolean isDebugLogLevel() {
        return debugLogLevel;
    }

    public void setDebugLogLevel(boolean value) {
        this.debugLogLevel = value;
    }

    public void logOutput(LoggerLevel level, String message) {
        // build the timestamp
        String timestamp = LocalTime.now().format(TIME_FORMATTER);

        switch (level) {
            case DEBUG -> {
                if (isValid(level)) {
                    logging.logToOutput(formatLine(timestamp, "Debug", message));
                }
            }
            case UI -> {
                if (fuzzerLogger != null) {
                    fuzzerLogger.accept(formatLine(timestamp, "Info", message));
                }
            }
            case DEFAULT -> {
                logging.logToOutput(message);
            }
        }
    }

    /**
     * Hex-dump variant simply builds the string then delegates.
     */
    public void logOutput(LoggerLevel level, ByteBuffer output) {
        byte[] bytes = output.array();
        StringBuilder hex = new StringBuilder();
        StringBuilder asc = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            if (i % BYTES_PER_LINE == 0) {
                if (i != 0) {
                    hex.append("  ").append(asc).append("\n");
                    asc.setLength(0);
                }
                hex.append(String.format("%08X: ", i));
            }

            hex.append(String.format("%02X ", bytes[i]));
            char c = (bytes[i] >= 32 && bytes[i] <= 126) ? (char) bytes[i] : '.';
            asc.append(c);

            if ((i + 1) % 8 == 0) hex.append(" ");
        }
        int rem = bytes.length % BYTES_PER_LINE;
        if (rem > 0) {
            int pad = (BYTES_PER_LINE - rem) * 3 + ((BYTES_PER_LINE - rem) / 8);
            hex.append(" ".repeat(pad)).append("  ").append(asc);
        }

        logOutput(level, hex.toString());
    }

    public void logOutput(String output) {
        // plain passthrough (no prefix)
        logging.logToOutput(output);
    }

    public void logError(String output) {
        logging.logToError(output);
    }

    public void raiseInfoEvent(String output) {
        logging.raiseInfoEvent(output);
    }

    public OutputStream outputStream() {
        return logging.output();
    }

    public OutputStream errorStream() {
        return logging.error();
    }

    private boolean isValid(LoggerLevel level) {
        return !level.equals(DEBUG) || debugLogLevel;
    }

    private String formatLine(String timestamp, String tool, String msg) {
        return String.format("[%s] [%s] %s", timestamp, tool, msg);
    }
}
