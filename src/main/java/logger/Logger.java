package logger;

import burp.api.montoya.logging.Logging;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static logger.LoggerLevel.DEBUG;

public class Logger {
    private static final int BYTES_PER_LINE = 16;
    private final Logging logging;
    private Consumer<String> listener;
    private boolean debugLogLevel;

    public Logger(Logging logging) {
        this.logging = logging;
        debugLogLevel = false;
    }

    public void registerListener(Consumer<String> listener) {
        this.listener = listener;
    }

    public boolean isDebugLogLevel() {
        return debugLogLevel;
    }

    public void setDebugLogLevel(boolean value) {
        debugLogLevel = value;
    }

    public void logOutput(LoggerLevel loggerLevel, String output) {
        switch (loggerLevel) {
            case DEBUG -> {
                if (isValid(loggerLevel)) logging.logToOutput(output);
            }
            case UI -> {
                if (listener != null) listener.accept(output);
            }
            case DEFAULT -> logging.logToOutput(output);
        }
    }
    public void logOutput(LoggerLevel loggerLevel, ByteBuffer output) {
        byte[] bytes = output.array();
        StringBuilder hexDump = new StringBuilder();
        StringBuilder asciiDump = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i % BYTES_PER_LINE == 0) {
                if (i != 0) {
                    hexDump.append("  ").append(asciiDump).append("\n");
                    asciiDump.setLength(0);
                }
                hexDump.append(String.format("%08X: ", i));
            }

            hexDump.append(String.format("%02X ", bytes[i]));

            char c = (bytes[i] >= 32 && bytes[i] <= 126) ? (char) bytes[i] : '.';
            asciiDump.append(c);

            if ((i + 1) % 8 == 0) {
                hexDump.append(" ");
            }
        }

        int remaining = bytes.length % BYTES_PER_LINE;
        if (remaining > 0) {
            int padding = (BYTES_PER_LINE - remaining) * 3 + ((BYTES_PER_LINE - remaining) / 8);
            hexDump.append(" ".repeat(padding)).append("  ").append(asciiDump);
        }
        logOutput(loggerLevel, hexDump.toString());
    }
    public void logOutput(String output) {
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

    private boolean isValid(LoggerLevel outputLogLevel) {
        return !outputLogLevel.equals(DEBUG) || debugLogLevel;
    }
}
