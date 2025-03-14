package embedded;

import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import logger.Logger;
import logger.LoggerLevel;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class EmbeddedServerExecutor {
    private final Logger logger;
    private final EmbeddedServerStatus serverStatus;
    private final Map<String, ConnectionWrapper> clients = new HashMap<>();
    private Tomcat tomcat;

    public EmbeddedServerExecutor(Logger logger, EmbeddedServerStatus serverStatus) {
        this.logger = logger;
        this.serverStatus = serverStatus;
    }

    public void startServer() {
        if (tomcat != null) {
            logger.logError("Server is already running!");
            return;
        }

        try {
            logger.logOutput(LoggerLevel.DEBUG, "Initializing Tomcat Embedded Server...");
            tomcat = new Tomcat();
            tomcat.setPort(9000);
            tomcat.getConnector();

            Path baseDir = Files.createTempDirectory("tomcat-embedded");
            tomcat.setBaseDir(baseDir.toAbsolutePath().toString());

            Context ctx = tomcat.addContext("", baseDir.toAbsolutePath().toString());

            // Register Servlet
            Tomcat.addServlet(ctx, "proxyServlet", new ProxyServlet(logger, clients));
            ctx.addServletMappingDecoded("/proxy", "proxyServlet");

            tomcat.start();
            logger.logOutput(LoggerLevel.DEBUG, "Tomcat Embedded Server started on port 9000...");
            new Thread(() -> tomcat.getServer().await()).start();
        } catch (IOException | LifecycleException e) {
            logger.logError("Failed to start Tomcat server: " + e.getMessage());
        }
    }

    public void processConnection(ConnectionWrapper connection) {
        if (connection.getStatus() == ExtensionWebSocketCreationStatus.STREAMING_RESPONSE) {
            clients.put(connection.getURL(), connection);
        }
    }

    public void stopServer() {
        if (tomcat == null) {
            logger.logOutput(LoggerLevel.DEBUG, "Server is already stopped.");
            return;
        }

        logger.logOutput(LoggerLevel.DEBUG, "Stopping Tomcat Server...");
        clients.forEach((key, client) -> client.stop());
        clients.clear();

        try {
            tomcat.stop();
            tomcat.destroy();
            tomcat = null;
            logger.logOutput(LoggerLevel.DEBUG, "Tomcat Server stopped.");
        } catch (LifecycleException e) {
            logger.logError("Error stopping Tomcat Server: " + e.getMessage());
        }
    }

    @WebServlet("/proxy")
    public static class ProxyServlet extends HttpServlet {
        private static final int TIMEOUT = 5;
        private final Logger logger;
        private final Map<String, ConnectionWrapper> clients;

        public ProxyServlet(Logger logger, Map<String, ConnectionWrapper> clients) {
            this.logger = logger;
            this.clients = clients;
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String query = request.getQueryString();
            String target = query != null && query.startsWith("url=") ? URLDecoder.decode(query.substring(4), StandardCharsets.UTF_8) : "";
            byte[] body = readRequestBody(request.getInputStream());

            try {
                if (clients.containsKey(target)) {
                    ByteBuffer wsResponse = clients.get(target).sendMessage(body, TIMEOUT);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentLength(wsResponse.remaining());
                    response.getOutputStream().write(wsResponse.array());
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No response from WebSocket server");
                }
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
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


