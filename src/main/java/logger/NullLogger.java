package logger;

import org.eclipse.jetty.util.log.Logger;

public class NullLogger implements Logger {
    private final String name;

    public NullLogger() {
        this.name = "null";
    }

    public NullLogger(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void warn(String msg, Object... args) { /* no-op */ }

    @Override
    public void warn(Throwable thrown) { /* no-op */ }

    @Override
    public void warn(String msg, Throwable thrown) { /* no-op */ }

    @Override
    public void info(String msg, Object... args) { /* no-op */ }

    @Override
    public void info(Throwable thrown) { /* no-op */ }

    @Override
    public void info(String msg, Throwable thrown) { /* no-op */ }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void setDebugEnabled(boolean enabled) { /* no-op */ }

    @Override
    public void debug(String msg, Object... args) { /* no-op */ }

    @Override
    public void debug(String s, long l) { /* no-op */ }

    @Override
    public void debug(Throwable thrown) { /* no-op */ }

    @Override
    public void debug(String msg, Throwable thrown) { /* no-op */ }

    @Override
    public Logger getLogger(String name) {
        return new NullLogger(name);
    }

    @Override
    public void ignore(Throwable throwable) { /* no-op */ }
}
