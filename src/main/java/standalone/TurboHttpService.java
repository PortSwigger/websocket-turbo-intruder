package standalone;

import burp.api.montoya.http.HttpService;

import java.net.URI;
import java.net.URISyntaxException;

public class TurboHttpService implements HttpService {
    private String host;
    private int port;
    private boolean isSecure;

    public TurboHttpService(String host, int port, boolean isSecure) {
        this.host = host;
        this.port = port;
        this.isSecure = isSecure;
    }

    public TurboHttpService(String url) {
        try {
            URI uri = new URI(url);
            this.host = validateHost(uri.getHost());
            this.isSecure = isHttps(uri.getScheme());
            this.port = resolvePort(uri.getPort(), this.isSecure);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private String validateHost(String host) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        return host;
    }

    private boolean isHttps(String scheme) {
        return "https".equalsIgnoreCase(scheme);
    }

    private int resolvePort(int port, boolean secure) {
        if (port > 0) {
            return port;
        }
        return secure ? 443 : 80;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public boolean secure() {
        return isSecure;
    }

    @Override
    public String ipAddress() {
        return "";
    }
}
