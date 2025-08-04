package embedded;

import attack.AttackState;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.websocket.WebSockets;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import connection.Connection;
import data.ConnectionMessage;
import logger.Logger;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static burp.api.montoya.websocket.Direction.SERVER_TO_CLIENT;

public class ConnectionWrapper {
    private final Logger logger;
    private final HttpRequest upgradeRequest;
    private final List<ByteArray> messages;
    private final String editorCodeString;

    private final Map<Long, CompletableFuture<ByteBuffer>> pendingRequests = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final EmbeddedServerExecutor serverExecutor;
    private final AttackState attackState;
    private Connection connection;

    public ConnectionWrapper(Logger logger,
                             Consumer<ConnectionMessage> historyMessageConsumer,
                             WebSockets websockets,
                             HttpRequest upgradeRequest,
                             List<ByteArray> messages,
                             String editorCodeString
    ) throws Exception {
        this.logger = logger;
        this.upgradeRequest = upgradeRequest;
        this.messages = messages;
        this.editorCodeString = editorCodeString;

        attackState = new AttackState();
        Consumer<ConnectionMessage> consumer = msg -> {
            if (msg.getDirection() == SERVER_TO_CLIENT) {
                long hash = msg.getHash();
                CompletableFuture<ByteBuffer> f = pendingRequests.remove(hash);
                if (f != null) {
                    f.complete(msg.getMessageByteBuffer());
                }
            }
        };
        serverExecutor = new EmbeddedServerExecutor(
                logger,
                websockets,
                historyMessageConsumer,
                consumer,
                attackState
        );

        start();
    }

    public ByteBuffer sendMessage(byte[] payload, int timeoutSeconds)
            throws InterruptedException, ExecutionException, TimeoutException {
        final long id;
        final CompletableFuture<ByteBuffer> cf = new CompletableFuture<>();

        synchronized (lock) {
            if (connection == null) throw new IllegalStateException("Connection was closed");
            id = connection.queue(payload);
            pendingRequests.put(id, cf);
        }

        try {
            return cf.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            logger.logError("Timeout waiting for WebSocket \"response\", reconnecting...");
            stop();
            start();
            throw te;
        } catch (ExecutionException | InterruptedException ee) {
            logger.logError("Send Message error, stopping...");
            stop();
            throw ee;
        } finally {
            pendingRequests.remove(id);
        }
    }

    private void start() {
        synchronized (lock) {
            attackState.newAttack();
            connection = serverExecutor.start(upgradeRequest, editorCodeString);
        }
    }

    public void stop() {
        synchronized (lock) {
            try {
                attackState.endAttack();
                if (serverExecutor != null) {
                    serverExecutor.stop();
                }
                connection = null;
            } catch (Exception e) {
                logger.logError("Error stopping WebSocket client");
            }
        }
        pendingRequests.forEach((k, f) ->
                f.completeExceptionally(new IllegalStateException("Connection reset")));
        pendingRequests.clear();
    }

    public HttpRequest getUpgradeRequest() {
        return upgradeRequest;
    }

    public String getURL() {
        try {
            return upgradeRequest.url();
        } catch (MalformedRequestException e) {
            return "";
        }
    }

    public List<ByteArray> getMessages() {
        return messages;
    }

    public ExtensionWebSocketCreationStatus getStatus() {
        return connection == null
                ? ExtensionWebSocketCreationStatus.CONNECTION_FAILED
                : connection.status();
    }

}