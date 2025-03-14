package embedded;

import java.util.concurrent.atomic.AtomicBoolean;

public class EmbeddedServerState implements EmbeddedServerStatus {
    private final AtomicBoolean isRunning;

    public EmbeddedServerState() {
        isRunning = new AtomicBoolean();
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    public void startServer() {
        isRunning.set(true);
    }

    void stopServer() {
        isRunning.set(false);
    }
}
