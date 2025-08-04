package embedded;

import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import logger.Logger;
import logger.LoggerLevel;
import logger.NullLogger;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import ui.editor.embedded.CustomErrorHandler;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static burp.WebSocketFuzzer.EXTENSION_NAME;

public class JettyEmbeddedServerExecutor {

    private final Logger logger;
    private final ServerDetails serverDetails;
    private final Map<String, ConnectionWrapper> clients = new ConcurrentHashMap<>();

    private final Object lock = new Object();
    private Server server;

    public JettyEmbeddedServerExecutor(Logger logger, ServerDetails serverDetails) {
        this.logger = logger;
        this.serverDetails = serverDetails;
    }

    public void start() {
        synchronized (lock) {
            if (server != null && server.isStarted()) {
                logger.logError("Embedded " + EXTENSION_NAME + " server is already running!");
                return;
            }

            logger.logOutput(LoggerLevel.DEBUG,
                    "Initializing embedded " + EXTENSION_NAME + " server on port " + serverDetails.getPort() + "...");

            Log.setLog(new NullLogger());

            QueuedThreadPool pool = new QueuedThreadPool();
            pool.setName("turbo-intruder-server-worker");
            pool.setDaemon(true);
            pool.setStopTimeout(500);

            server = new Server(pool);

            server.addBean(new CustomErrorHandler());
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setSendServerVersion(false);

            HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);

            ServerConnector connector = new ServerConnector(server, connectionFactory);
            connector.setHost("127.0.0.1");
            connector.setPort(serverDetails.getPort());
            server.addConnector(connector);

            Filter disableTraceFilter = new Filter() {
                @Override
                public void init(FilterConfig filterConfig) {
                }

                @Override
                public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                        throws IOException, ServletException {
                    if (request instanceof HttpServletRequest httpRequest) {
                        String method = httpRequest.getMethod();
                        if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, " method not allowed");
                            return;
                        }
                    }
                    chain.doFilter(request, response);
                }

                @Override
                public void destroy() {
                }
            };

            Filter authFilter = new Filter() {
                @Override public void init(FilterConfig filterConfig) {}
                @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                        throws IOException, ServletException {
                    if (request instanceof HttpServletRequest httpReq) {
                        String authHeader = httpReq.getHeader("Authorization");
                        String expected = serverDetails.getSecret();
                        if (authHeader == null || !authHeader.equals(expected)) {
                            ((HttpServletResponse) response)
                                    .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing Authorization");
                            return;
                        }
                    }
                    chain.doFilter(request, response);
                }
                @Override public void destroy() {}
            };

            Filter cspFilter = new Filter() {
                @Override public void init(FilterConfig filterConfig) {}

                @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                        throws IOException, ServletException {
                    if (response instanceof HttpServletResponse httpResp) {
                        httpResp.setHeader(
                                "Content-Security-Policy",
                                "default-src 'none'; connect-src 'self';"
                        );
                    }
                    chain.doFilter(request, response);
                }

                @Override public void destroy() {}
            };

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            context.setContextPath("/");
            context.addFilter(new FilterHolder(authFilter),      "/*", EnumSet.of(DispatcherType.REQUEST));
            context.addFilter(new FilterHolder(disableTraceFilter),"/*", EnumSet.of(DispatcherType.REQUEST));
            context.addFilter(new FilterHolder(cspFilter),       "/*", EnumSet.of(DispatcherType.REQUEST));
            context.setClassLoader(getClass().getClassLoader());

            ServletHolder proxyHolder = new ServletHolder(new ProxyServlet());
            context.addServlet(proxyHolder, "/proxy");

            ContextHandlerCollection handlers = new ContextHandlerCollection();
            handlers.addHandler(context);
            server.setHandler(handlers);

            try {
                server.start();
                logger.logOutput(LoggerLevel.DEBUG,
                        "Embedded " + EXTENSION_NAME + " server started on port " + serverDetails.getPort());
            } catch (Exception e) {
                logger.logError("Failed to start embedded server: " + e.getMessage());
                throw new IllegalArgumentException(e);
            }
        }
    }

    public void processConnection(ConnectionWrapper connection) {
        if (connection.getStatus() == ExtensionWebSocketCreationStatus.SUCCESS) {
            clients.put(connection.getURL(), connection);
        }
    }

    public void stop() {
        synchronized (lock) {
            if (server == null || server.isStopped()) {
                logger.logOutput(LoggerLevel.DEBUG, "Embedded " + EXTENSION_NAME + " server is already stopped.");
                return;
            }

            clients.values().forEach(ConnectionWrapper::stop);
            clients.clear();

            try {
                logger.logOutput(LoggerLevel.DEBUG, "Stopping...");
                server.setStopTimeout(500);
                server.stop();
                server.join();
            } catch (Exception e) {
                logger.logError("Error stopping embedded " + EXTENSION_NAME + " server" + e.getMessage());
            }

            QueuedThreadPool pool = (QueuedThreadPool) server.getThreadPool();
            try {
                pool.stop();
            } catch (Exception e) {
                logger.logError("Error stopping thread pool" + e.getMessage());
            }

            server.destroy();
            server = null;
            logger.logOutput(LoggerLevel.DEBUG, "Embedded" + EXTENSION_NAME + "server fully stopped.");
        }
    }

    @WebServlet("/proxy")
    public class ProxyServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            String target = req.getParameter("url");
            if (target == null || target.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
                return;
            }

            byte[] body = req.getInputStream().readAllBytes();

            try {
                ConnectionWrapper client = clients.get(target);
                if (client == null) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "No WebSocket response for " + target);
                    return;
                }

                ByteBuffer wsResponse = client.sendMessage(body, serverDetails.getTimeout());
                int len = wsResponse.remaining();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentLength(len);
                resp.getOutputStream().write(wsResponse.array(), wsResponse.position(), len);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error proxying to WebSocket");
            }
        }
    }
}