package embedded;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerStatus {
    private final AtomicBoolean isRunning;

    public ServerStatus() {
        isRunning = new AtomicBoolean();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void start() {
        isRunning.set(true);
    }

    public void stop() {
        isRunning.set(false);
    }

}
