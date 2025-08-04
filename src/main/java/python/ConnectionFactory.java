package python;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.WebSockets;
import connection.Connection;
import connection.ThreadedTurboWebSocketConnection;
import connection.TurboWebSocketConnection;
import connection.WebSocketConnection;
import data.Engine;
import data.PendingMessages;
import logger.Logger;
import socket.TurboConfig;
import socket.TurboConnectionPublisher;
import socket.TurboWebSockets;

/**
 * Factory class for creating WebSocket connections with different engine types and configurations.
 * This class is primarily called from Python scripts via Jython to establish WebSocket connections
 * for fuzzing and testing operations.
 *
 * <p>The factory supports three main engine types:
 * <ul>
 *   <li><b>TURBO</b> - High-performance single-threaded connection using optimized I/O</li>
 *   <li><b>THREADED</b> - Multi-threaded connection for concurrent operations</li>
 *   <li><b>Default</b> - Standard WebSocket connection using Burp's native WebSocket API</li>
 * </ul>
 *
 * <p>Usage from Python scripts:
 * <pre>{@code
 * # Create a connection with default engine
 * connection = websocket_connection.create(upgrade_request)
 *
 * # Create a connection with specific engine
 * connection = websocket_connection.create(upgrade_request, Engine.TURBO)
 *
 * # Create a connection with custom configuration
 * config = websocket_connection.config(5000, 100, 3, 10, "hello", True, False)
 * connection = websocket_connection.create(upgrade_request, Engine.TURBO, config)
 * }</pre>
 */
public class ConnectionFactory {
    private final Logger logger;
    private final WebSockets webSockets;
    private final PendingMessages pendingMessages;
    private final int attackId;
    private final TurboConnectionPublisher publisher;

    /**
     * Constructs a new ConnectionFactory with the required dependencies.
     *
     * @param logger          the logger instance for connection events and errors
     * @param webSockets      the Burp WebSockets API instance
     * @param pendingMessages the message queue for managing outgoing messages
     * @param attackId        the unique identifier for this attack session
     * @param publisher       the connection publisher for broadcasting connection events
     */
    public ConnectionFactory(
            Logger logger,
            WebSockets webSockets,
            PendingMessages pendingMessages,
            int attackId,
            TurboConnectionPublisher publisher
    ) {
        this.logger = logger;
        this.webSockets = webSockets;
        this.pendingMessages = pendingMessages;
        this.attackId = attackId;
        this.publisher = publisher;
    }

    /**
     * Creates a connection using the appropriate engine based on the WebSockets instance type.
     * If TurboWebSockets is available, creates a TURBO connection; otherwise creates a standard connection.
     *
     * @param upgradeRequest the HTTP request used to upgrade to WebSocket
     * @return a Connection instance ready for message operations
     * @throws Exception if connection creation fails
     */
    private Connection connection(HttpRequest upgradeRequest) throws Exception {
        if (webSockets instanceof TurboWebSockets)
            return new TurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, TurboConfig.turbo());
        return new WebSocketConnection(logger, webSockets, pendingMessages, attackId, upgradeRequest, publisher);
    }

    /**
     * Creates a WebSocket connection using the default engine selection logic.
     * This method is called directly from Python scripts.
     *
     * @param upgradeRequest the HTTP request to upgrade to WebSocket protocol
     * @return a Connection instance for sending and receiving WebSocket messages
     * @throws Exception if the connection cannot be established
     */
    @SuppressWarnings("unused") // called by Python
    public Connection create(HttpRequest upgradeRequest) throws Exception {
        return connection(upgradeRequest);
    }

    /**
     * Creates a WebSocket connection with a specific engine type and configuration.
     * This method provides full control over connection parameters and is called from Python scripts.
     *
     * @param upgradeRequest the HTTP request to upgrade to WebSocket protocol
     * @param engine         the engine type (TURBO, THREADED, or null for default)
     * @param config         the TurboConfig object with connection parameters
     * @return a Connection instance configured according to the specified parameters
     * @throws Exception if the connection cannot be established
     */
    @SuppressWarnings("unused") // called by Python
    public Connection create(HttpRequest upgradeRequest, Engine engine, TurboConfig config) throws Exception {
        switch (engine) {
            case TURBO -> {
                return new TurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, config);
            }
            case THREADED -> {
                return new ThreadedTurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, config);
            }
            case null, default -> {
                return connection(upgradeRequest);
            }
        }
    }

    /**
     * Creates a WebSocket connection with a specific engine type using default configuration.
     * This method is convenient when you want to specify the engine but use default settings.
     *
     * @param upgradeRequest the HTTP request to upgrade to WebSocket protocol
     * @param engine         the engine type (TURBO for high-performance, THREADED for concurrency, null for default)
     * @return a Connection instance using the specified engine with default configuration
     * @throws Exception if the connection cannot be established
     */
    public Connection create(HttpRequest upgradeRequest, Engine engine) throws Exception {
        switch (engine) {
            case TURBO -> {
                return new TurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, TurboConfig.turbo());
            }
            case THREADED -> {
                return new ThreadedTurboWebSocketConnection(logger, pendingMessages, attackId, upgradeRequest, publisher, TurboConfig.threaded());
            }
            case null, default -> {
                return connection(upgradeRequest);
            }
        }
    }

    /**
     * Creates a default TurboConfig instance with standard settings.
     * This method is called from Python scripts to get a basic configuration object.
     *
     * @return a TurboConfig with default connection parameters
     */
    @SuppressWarnings("unused") // called by Python
    public TurboConfig config() {
        return new TurboConfig();
    }

    /**
     * Creates a customized TurboConfig instance with specific parameters.
     * This method allows Python scripts to fine-tune connection behavior.
     *
     * @param connectionTimeout timeout in milliseconds for establishing connections
     * @param sendingInterval   interval in milliseconds between sending messages
     * @param retries           number of retry attempts for failed operations
     * @param nThreads          number of threads to use for multi-threaded operations
     * @param helloMessage      initial message to send after connection establishment
     * @param isPinging         whether to enable automatic ping/pong keep-alive mechanism
     * @param useChunks         whether to use chunked message sending for large payloads
     * @return a TurboConfig configured with the specified parameters
     */
    @SuppressWarnings("unused") // called by Python
    public TurboConfig config(int connectionTimeout, int sendingInterval, int retries, int nThreads, String helloMessage, boolean isPinging, boolean useChunks) {
        return new TurboConfig(connectionTimeout, sendingInterval, retries, nThreads, helloMessage, isPinging, useChunks);
    }
}
