package data;

import attack.AttackStatus;
import burp.api.montoya.websocket.Direction;
import logger.Logger;
import utils.Utilities;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PendingMessages implements Consumer<AttackIdAndWebSocketConnectionMessage>, Supplier<ConnectionMessage> // TODO
{
    private final ConcurrentHashMap<Long, ConnectionMessage> unprocessedTimeQueue;
    private final BlockingQueue<ConnectionMessage> unprocessedMessageQueue;
//    private final BlockingQueue<SafeResponseAttributes> baselineMessageQueue;
    private final Logger logger;
    private final AttackStatus attackStatus;
    private final AtomicInteger lastOut = new AtomicInteger(0);
    private final AtomicInteger lastIn = new AtomicInteger(0);
    private final CustomWSMessageGroup messageGroup;
    private Consumer<String> listener;
    private long lastHash;

    public PendingMessages(Logger logger, AttackStatus attackStatus) {
        this.unprocessedTimeQueue = new ConcurrentHashMap<>();
        this.logger = logger;
        this.attackStatus = attackStatus;
        this.unprocessedMessageQueue = new LinkedBlockingQueue<>();
//        this.baselineMessageQueue = new LinkedBlockingQueue<>();
        this.messageGroup = new CustomWSMessageGroup(Utilities::calculateFingerprint);
    }

    private void progressBar(ConnectionMessage message) {
        Progress progress = new Progress(
                lastIn.get(),
                lastOut.get(),
                lastHash,
                message.getMessageLength(),
                0,
//                message.getWordCount(),
                message.getTime(),
                message.getDateTime());
        if (listener != null) listener.accept(progress.toString());
    }

    private void hasMessageInQueue(ConnectionMessage message) {
        Map.Entry<Long, ConnectionMessage> found = null;
        for (Map.Entry<Long, ConnectionMessage> unprocessed : unprocessedTimeQueue.entrySet()) {
            if (unprocessed.getValue().getHash() == message.getHash()) {
                found = unprocessed;
                break;
            }
        }
        if (found != null) {
            unprocessedTimeQueue.remove(found.getKey());
            long time = message.getDateTimeInMilliseconds() - found.getValue().getDateTimeInMilliseconds();
            message.setTime(time);
        }
    }

    private boolean shouldProcess(ConnectionMessage message) {
        return messageGroup.getFirstMessage() == null || !messageGroup.matches(message);
    }

    private ConnectionMessage updateMessage(ConnectionMessage message) {
        if (message.getDirection() == Direction.CLIENT_TO_SERVER) {
            lastIn.incrementAndGet();
            unprocessedTimeQueue.put(message.getId(), message);
        } else {
            lastOut.incrementAndGet();
            lastHash = message.getId();
            hasMessageInQueue(message);
            if (shouldProcess(message)) {
                message.setInteresting(true);
//                message.setComment(messageGroup.describeDiff(message));
                messageGroup.add(message);
            }
        }
        return message;
    }
//    private ConnectionMessage updateMessage(ConnectionMessage message) {
//        if (message.getDirection() == Direction.CLIENT_TO_SERVER) {
//            lastIn.incrementAndGet();
//            unprocessedTimeQueue.put(message.getId(), message);
//        } else {
//            lastOut.incrementAndGet();
//            lastHash = message.getId();
//            hasMessageInQueue(message);
//            SafeResponseAttributes baseMessage = new SafeResponseAttributes(message);
//            message.setAttributes(baseMessage.getAttributes());
//            if (!baselineMessageQueue.contains(baseMessage)) {
//                baselineMessageQueue.add(baseMessage);
//                message.setInteresting(true);
//            }
//        }
//        return message;
//    }

    @Override
    public void accept(AttackIdAndWebSocketConnectionMessage attackIdAndWebSocketConnectionMessage) {
        if (attackStatus.isRunning() && attackStatus.isCurrentAttackId(attackIdAndWebSocketConnectionMessage.attackId())) {
            ConnectionMessage message = updateMessage(attackIdAndWebSocketConnectionMessage.connectionMessage());
            unprocessedMessageQueue.add(message);
            progressBar(message);
        }
    }

    @Override
    public ConnectionMessage get() {
        try {
            return unprocessedMessageQueue.take();
        } catch (InterruptedException e) {
            if (attackStatus.isRunning()) {
                logger.logError("Error taking from pending messages.");
            }

            return null;
        }
    }

    public void clear() {
        unprocessedTimeQueue.clear();
        unprocessedMessageQueue.clear();
//        baselineMessageQueue.clear();
        messageGroup.reset();
        lastIn.set(0);
        lastOut.set(0);
    }

    public void registerListener(Consumer<String> listener) {
        this.listener = listener;
    }
}
