package attack;

import data.AttackDetails;
import data.ConnectionMessage;
import data.MessagesToDisplay;
import data.PendingMessages;
import logger.Logger;
import queue.SendMessageQueueConsumer;
import queue.TableBlockingQueueConsumer;
import turbo.TurboThreadExecutor;

import java.util.function.Consumer;

import static logger.LoggerLevel.DEBUG;
import static logger.LoggerLevel.UI;

public class AttackManager implements AttackStarter, AttackStopper {
    private final Logger logger;
    private final PendingMessages pendingMessages;
    private final MessagesToDisplay messagesToDisplay;
    private final Consumer<ConnectionMessage> historyMessageConsumer;
    private final Consumer<ConnectionMessage> connectionMessageConsumer;
    private final AttackScriptExecutor scriptExecutor;
    private final AttackState attackState;

    private TurboThreadExecutor sendMessageExecutorService;
    private TurboThreadExecutor tableExecutorService;

    public AttackManager(
            Logger logger,
            PendingMessages pendingMessages,
            MessagesToDisplay messagesToDisplay,
            Consumer<ConnectionMessage> historyMessageConsumer,
            Consumer<ConnectionMessage> messageConsumer,
            AttackScriptExecutor scriptExecutor,
            AttackState attackState
    ) {
        this.logger = logger;
        this.pendingMessages = pendingMessages;
        this.messagesToDisplay = messagesToDisplay;
        this.historyMessageConsumer = historyMessageConsumer;
        this.connectionMessageConsumer = messageConsumer;
        this.scriptExecutor = scriptExecutor;
        this.attackState = attackState;
    }

    @Override
    public void startAttack(AttackDetails attackDetails) {
        attackState.newAttack();

        sendMessageExecutorService = new TurboThreadExecutor(logger, attackDetails.numberOfThreads());
        sendMessageExecutorService.submitTask(
                new SendMessageQueueConsumer(
                        historyMessageConsumer,
                        scriptExecutor::processMessage,
                        attackState,
                        pendingMessages
                )
        );

        logger.logOutput(UI,
                String.format("Attack %d. Number of threads attack started with: %d",
                        attackState.currentAttackId(),
                        attackDetails.numberOfThreads()));

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

        if (sendMessageExecutorService != null) sendMessageExecutorService.shutdown();

        if (tableExecutorService != null) tableExecutorService.shutdown();

        if (pendingMessages != null) pendingMessages.clear();
        if (messagesToDisplay != null) messagesToDisplay.clear();
    }

    public void registerProgressListener(Consumer<String> listener) {
        this.pendingMessages.registerListener(listener);
    }
}
