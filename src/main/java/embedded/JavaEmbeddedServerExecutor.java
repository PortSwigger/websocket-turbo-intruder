package embedded;

import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import logger.Logger;
import logger.LoggerLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class JavaEmbeddedServerExecutor {
    private final Logger logger;
    private final EmbeddedServerStatus serverStatus;
    private final Map<String, ConnectionWrapper> clients = new HashMap<>();
    private HttpServer server;

    public JavaEmbeddedServerExecutor(Logger logger, EmbeddedServerStatus serverStatus) {
        this.logger = logger;
        this.serverStatus = serverStatus;
    }

    public void startServer() {
        if (server != null) {
            logger.logError("Server is already running!");
            return;
        }

        try {
            logger.logOutput(LoggerLevel.DEBUG, "Initializing HTTP Server...");
            server = HttpServer.create(new InetSocketAddress(9000), 0);
            server.createContext("/proxy", new ProxyHandler(logger, clients));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            logger.logOutput(LoggerLevel.DEBUG, "HTTP Server started on port 9000, awaiting requests...");
        } catch (IOException e) {
            logger.logError("Failed to start server: " + e.getMessage());
        }
    }

    public void processConnection(ConnectionWrapper connection) {
        if (connection.getStatus() == ExtensionWebSocketCreationStatus.STREAMING_RESPONSE) {
            clients.put(connection.getURL(), connection);
        }
    }

    public void stopServer() {
        if (server == null) {
            logger.logOutput(LoggerLevel.DEBUG, "Server is already stopped.");
            return;
        }

        logger.logOutput(LoggerLevel.DEBUG, "Stopping HTTP Server...");
        clients.forEach((key, client) -> {
            client.stop();
        });
        clients.clear();
        server.stop(0);
        server = null;
        logger.logOutput(LoggerLevel.DEBUG, "HTTP Server stopped.");
    }

    private class ProxyHandler implements HttpHandler {
        private static final int TIMEOUT = 5;
        private final Logger logger;
        private final Map<String, ConnectionWrapper> clients;

        public ProxyHandler(Logger logger, Map<String, ConnectionWrapper> clients) {
            this.logger = logger;
            this.clients = clients;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String target = query != null && query.startsWith("url=") ? query.substring(4) : "";
            byte[] body = readRequestBody(exchange.getRequestBody());

            try {
                if (clients.containsKey(target)) {
                    ByteBuffer wsResponse = clients.get(target).sendMessage(body, TIMEOUT);
                    exchange.sendResponseHeaders(200, wsResponse.remaining());
                    OutputStream os = exchange.getResponseBody();
                    os.write(wsResponse.array());
                    os.close();
                } else {
                    exchange.sendResponseHeaders(500, 0);
                    OutputStream os = exchange.getResponseBody();
                    os.write("No response from WebSocket server".getBytes());
                    os.close();
                }
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, 0);
                OutputStream os = exchange.getResponseBody();
                os.write("Internal Server Error".getBytes());
                os.close();
            }

        }

        private byte[] readRequestBody(InputStream inputStream) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] temp = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(temp)) != -1) {
                buffer.write(temp, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }
}


