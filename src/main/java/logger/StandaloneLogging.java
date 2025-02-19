package logger;

import burp.api.montoya.logging.Logging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.*;


import java.io.PrintStream;
import java.util.logging.Logger;

public class StandaloneLogging implements Logging {
//    private static final Logger logger = Logger.getLogger("WebSocketFuzzer");

    private static final PrintStream outputStream = System.out;
    private static final PrintStream errorStream = System.err;
    private static final Object LOCK = new Object();
    private static final String CLEAR_LINE = "\033[2K"; // Clears the current line
    private static final String MOVE_UP = "\033[F"; // Moves cursor up
    private static final String MOVE_DOWN = "\033[E"; // Moves cursor down

    public StandaloneLogging() {
    }

    @Override
    public PrintStream output() {
        return outputStream;
    }

    @Override
    public PrintStream error() {
        return errorStream;
    }

    @Override
    public void logToOutput(String message) {
        synchronized (LOCK) {
            outputStream.print(MOVE_UP);
            outputStream.print(CLEAR_LINE);
            outputStream.println(message);
            outputStream.print(MOVE_DOWN);
            outputStream.flush();
        }
    }

    @Override
    public void logToError(String message) {
        errorStream.println(message);
    }

    @Override
    public void logToError(String message, Throwable cause) {
        errorStream.println(message);
    }

    @Override
    public void logToError(Throwable cause) {
        errorStream.println(cause.getMessage());
    }

    @Override
    public void raiseDebugEvent(String message) {
        errorStream.println(message);
    }

    @Override
    public void raiseInfoEvent(String message) {
        synchronized (LOCK) {
            outputStream.print(CLEAR_LINE);
            outputStream.printf("\r%s", message);
            outputStream.flush();
        }
    }

    @Override
    public void raiseErrorEvent(String message) {
        errorStream.println(message);
    }

    @Override
    public void raiseCriticalEvent(String message) {
        errorStream.println(message);
    }
}
