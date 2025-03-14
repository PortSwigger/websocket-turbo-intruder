package socket;

public record TurboConfig(
        int connectionTimeout,
        int sendingInterval,
        int retries,
        int nThreads,
        String helloMessage,
        boolean isPinging,
        boolean useChunks) {

    public TurboConfig() {
        this(1, 10, 1, 1, null, false, true);
    }

    public static TurboConfig turbo() {
        return new TurboConfig();
    }

    public static TurboConfig threaded() {
        return new TurboConfig(0, 10, 1, 10, null, true, true);
    }
}
