package embedded;

import attack.AttackState;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import connection.TurboWebSocketConnection;
import data.ConnectionMessage;
import data.FrameType;
import data.PendingMessages;
import logger.Logger;
import queue.SendMessageQueueConsumer;
import socket.TurboConfig;
import socket.TurboWebSocketConnectionMessage;
import socket.TurboWebSocketMessagePublisher;
import turbo.TurboThreadExecutor;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static burp.api.montoya.websocket.Direction.SERVER_TO_CLIENT;

public class ConnectionWrapper {
    private final HttpRequest upgradeRequest;
    private final AttackState attackState;
    private final Logger logger;
    private final TurboWebSocketConnection wsClient;
    private final TurboThreadExecutor sendMessageExecutorService;
    private final Object lock = new Object();
    private final List<String> filters;
    private final List<ByteArray> messages;
    private final ConcurrentSkipListMap<Long, CompletableFuture<ByteBuffer>> pendingRequests = new ConcurrentSkipListMap<>();

    public ConnectionWrapper(Logger logger, HttpRequest upgradeRequest, List<ByteArray> messages, List<String> filters) throws Exception {
        this.logger = logger;
        this.upgradeRequest = upgradeRequest;
        this.messages = messages;
        this.filters = filters;

        attackState = new AttackState();
        attackState.newAttack();

        PendingMessages pendingMessages = new PendingMessages(logger, attackState);
        TurboWebSocketMessagePublisher publisher = new TurboWebSocketMessagePublisher();

        this.wsClient = new TurboWebSocketConnection(logger, pendingMessages, attackState.currentAttackId(), upgradeRequest, publisher,
                new TurboConfig(1, 10, Integer.MAX_VALUE, 1, null, false, false));
        sendMessageExecutorService = new TurboThreadExecutor(logger);
        Consumer<ConnectionMessage> messageProcessor = new Consumer<ConnectionMessage>() {
            private boolean filterMessage(ConnectionMessage message) {
                return filters.stream().noneMatch(filter -> message.getMessage().contains(filter));
            }

            @Override
            public void accept(ConnectionMessage message) {
                if (message.getDirection() == SERVER_TO_CLIENT) {
                    if (filterMessage(message)) {
                        if (!pendingRequests.isEmpty() && pendingRequests.containsKey(message.getHash()))
                            pendingRequests.get(message.getHash()).complete(message.getMessageByteBuffer());
//                        if (message.getComment().startsWith(CONNECTION_CLOSED) && !pendingRequests.isEmpty()) {
//                            Map.Entry<Long, CompletableFuture<ByteBuffer>> entry = pendingRequests.pollFirstEntry();
//                            if (entry != null) entry.getValue().complete(ByteBuffer.wrap(message.getComment().getBytes(StandardCharsets.UTF_8)));
//
//                        }
                    }
                }
            }
        };
        sendMessageExecutorService.submitTask(
                new SendMessageQueueConsumer(
                        messageProcessor,
                        attackState,
                        pendingMessages
                )
        );
    }

    public ByteBuffer sendMessage(byte[] payload, int timeout) throws Exception {
        synchronized (lock) {
            TurboWebSocketConnectionMessage message = wsClient.createMessage(FrameType.TEXT.value, payload, (long) payload.length);
            wsClient.queueInternal(message);

            CompletableFuture<ByteBuffer> responseFuture = new CompletableFuture<>();
            Long id = message.getHash();
            pendingRequests.put(id, responseFuture);
            try {
                return responseFuture.get(timeout, TimeUnit.SECONDS);
            } finally {
                pendingRequests.remove(id);
            }
        }
    }


    public HttpRequest getUpgradeRequest() {
        return this.upgradeRequest;
    }

    public String getURL() {
        return upgradeRequest.url();
    }

    public List<ByteArray> getMessages() {
        return this.messages;
    }

    public ExtensionWebSocketCreationStatus getStatus() {
        if (wsClient == null) return ExtensionWebSocketCreationStatus.CONNECTION_FAILED;
        return wsClient.status();
    }

    public void stop() {
        this.wsClient.stop();
    }

}
