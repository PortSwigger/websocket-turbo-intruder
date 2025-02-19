package attack;

import burp.api.montoya.collaborator.CollaboratorClient;
import burp.api.montoya.collaborator.Interaction;
import data.AttackDetails;
import data.ConnectionMessage;
import data.MessagesToDisplay;
import data.PendingMessages;
import logger.Logger;
import queue.SendMessageQueueConsumer;
import queue.TableBlockingQueueConsumer;
import turbo.TurboThreadExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.time.LocalTime.now;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static logger.LoggerLevel.DEBUG;
import static logger.LoggerLevel.UI;

public class AttackManager implements AttackStarter, AttackStopper {
    private long startTime;
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
        startTime = System.currentTimeMillis();
        String message = String.format("Number of threads attack started with: %d, Time: %d", attackDetails.numberOfThreads(), startTime);
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
