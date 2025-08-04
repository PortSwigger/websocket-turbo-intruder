package embedded;


public class ServerDetails {
    private int port;
    private int timeout;

    private String script;
    private String secret;

    public ServerDetails(int port, int timeout, String script, String secret) {
        this.port = port;
        this.timeout = timeout;
        this.script = script;
        this.secret = secret;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getSecret() {
        return secret;
    }
}