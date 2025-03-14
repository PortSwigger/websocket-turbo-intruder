package attack;

import data.AttackDetails;
import data.ConnectionMessage;
import data.MessagesToDisplay;
import data.PendingMessages;
import logger.Logger;
import queue.SendMessageQueueConsumer;
import queue.TableBlockingQueueConsumer;
import turbo.TurboThreadExecutor;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import static java.time.LocalTime.now;
import static logger.LoggerLevel.DEBUG;
import static logger.LoggerLevel.UI;

public class AttackManager implements AttackStarter, AttackStopper {
    private final Logger logger;
    private final PendingMessages pendingMessages;
    private final MessagesToDisplay messagesToDisplay;
    private final Consumer<ConnectionMessage> connectionMessageConsumer;
    private final AttackScriptExecutor scriptExecutor;
    private final AttackState attackState;

    private TurboThreadExecutor sendMessageExecutorService;
    private TurboThreadExecutor tableExecutorService;

    public AttackManager(
            Logger logger,
            PendingMessages pendingMessages,
            MessagesToDisplay messagesToDisplay,
            Consumer<ConnectionMessage> messageConsumer,
            AttackScriptExecutor scriptExecutor,
            AttackState attackState
    ) {
        this.logger = logger;
        this.pendingMessages = pendingMessages;
        this.messagesToDisplay = messagesToDisplay;
        this.connectionMessageConsumer = messageConsumer;
        this.scriptExecutor = scriptExecutor;
        this.attackState = attackState;
    }

    @Override
    public void startAttack(AttackDetails attackDetails) {
        attackState.newAttack();

//        sendMessageExecutorService = newFixedThreadPool(attackDetails.numberOfThreads());
        sendMessageExecutorService = new TurboThreadExecutor(logger, attackDetails.numberOfThreads());
        sendMessageExecutorService.submitTask(
                new SendMessageQueueConsumer(
                        scriptExecutor::processMessage,
                        attackState,
                        pendingMessages
                )
        );
        String message = String.format("[%s] Number of threads attack started with: %d",
                now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                attackDetails.numberOfThreads());
        logger.logOutput(UI, new String(new char[message.length()]).replace("\0", "—"));
        logger.logOutput(UI, message);

        tableExecutorService = new TurboThreadExecutor(logger);
        tableExecutorService.submitTask(
                new TableBlockingQueueConsumer(
                        messagesToDisplay,
                        attackState,
                        connectionMessageConsumer
                )
        );
        logger.logOutput(DEBUG, "Table thread started.");

        scriptExecutor.startAttack(attackDetails.payload(), attackDetails.upgradeRequest(), attackDetails.script());
    }

    @Override
    public void stopAttack() {
        attackState.endAttack();

        scriptExecutor.stopAttack();

        sendMessageExecutorService.shutdown();

        tableExecutorService.shutdown();

        pendingMessages.clear();
        messagesToDisplay.clear();
    }

    public void registerListener(Consumer<String> listener) {
        this.logger.registerListener(listener);
    }

    public void registerProgressListener(Consumer<String> listener) {
        this.pendingMessages.registerListener(listener);
    }
}
