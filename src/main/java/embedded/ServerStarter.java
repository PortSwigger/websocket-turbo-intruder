package embedded;

public interface ServerStarter {
    void startServer(ServerDetails serverDetails);

    String uri();
}
