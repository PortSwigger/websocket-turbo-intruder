package embedded;

import attack.AttackState;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.WebSockets;
import connection.Connection;
import data.ConnectionMessage;
import data.DecoratedConnectionMessage;
import data.MessagesToDisplay;
import data.PendingMessages;
import interpreter.Interpreter;
import logger.Logger;
import logger.LoggerLevel;
import org.python.core.PyObject;
import python.ConnectionFactoryFactory;
import python.ResultsTable;
import queue.SendMessageQueueConsumer;
import queue.TableBlockingQueueConsumer;
import socket.TurboConnectionPublisher;
import turbo.TurboThreadExecutor;
import utils.Utilities;

import java.util.List;
import java.util.function.Consumer;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;

public class EmbeddedServerExecutor {
    private final Logger logger;
    private final ConnectionFactoryFactory connectionFactoryFactory;
    private final Consumer<ConnectionMessage> historyMessageConsumer;
    private final Consumer<ConnectionMessage> messageConsumer;
    private final MessagesToDisplay messagesToDisplay;

    private final AttackState attackState;
    private final PendingMessages pendingMessages;
    private final TurboConnectionPublisher publisher;

    private TurboThreadExecutor sendMessageExecutorService;
    private TurboThreadExecutor tableExecutorService;

    private Interpreter interpreter;

    public EmbeddedServerExecutor(
            Logger logger,
            WebSockets webSockets,
            Consumer<ConnectionMessage> historyMessageConsumer,
            Consumer<ConnectionMessage> messageConsumer,
            AttackState attackState
    ) {
        this.logger = logger;
        this.historyMessageConsumer = historyMessageConsumer;
        this.messageConsumer = messageConsumer;
        this.attackState = attackState;

        messagesToDisplay = new MessagesToDisplay(logger, attackState);
        pendingMessages = new PendingMessages(logger, attackState);
        publisher = new TurboConnectionPublisher();

        this.connectionFactoryFactory = new ConnectionFactoryFactory(
                logger,
                webSockets,
                pendingMessages,
                attackState::currentAttackId,
                publisher
        );

    }

    public Connection start(HttpRequest upgradeRequest, String editorCodeString) {

        sendMessageExecutorService = new TurboThreadExecutor(logger);
        sendMessageExecutorService.submitTask(
                new SendMessageQueueConsumer(
                        historyMessageConsumer,
                        this::processMessage,
                        attackState,
                        pendingMessages
                )
        );

        tableExecutorService = new TurboThreadExecutor(logger);
        tableExecutorService.submitTask(
                new TableBlockingQueueConsumer(
                        messagesToDisplay,
                        attackState,
                        messageConsumer
                )
        );

        try {
            String environment = Utilities.loadScriptEnvironment();
            interpreter = new Interpreter(logger);
            interpreter.setVariable("websocket_connection", connectionFactoryFactory.create());
            interpreter.setVariable("results_table", new ResultsTable(messagesToDisplay));
            interpreter.setVariable("upgrade_request", upgradeRequest);

            interpreter.execute(environment);
            interpreter.execute(editorCodeString);

            PyObject conn = interpreter.call("create_connection", List.of("upgrade_request"));
            return (Connection) conn.__tojava__(Connection.class);

        } catch (Exception e) {
            logger.logOutput(LoggerLevel.UI, "Jython code error. Please review.\r\n" + e);

            interpreter.close();

            throw new IllegalArgumentException(e);
        }
    }

    public void processMessage(ConnectionMessage webSocketConnectionMessage) {

        String messageParameterName = "websocket_message";
        interpreter.setVariable(messageParameterName, new DecoratedConnectionMessage(webSocketConnectionMessage));

        String callbackMethod = webSocketConnectionMessage.getDirection() == CLIENT_TO_SERVER
                ? "handle_outgoing_message"
                : "handle_incoming_message";
        interpreter.execute(String.format("%s(%s)", callbackMethod, messageParameterName));
    }

    public void stop() {

        if (publisher != null) publisher.close();
        if (interpreter != null) interpreter.close();
        if (sendMessageExecutorService != null) sendMessageExecutorService.shutdown();
        if (tableExecutorService != null) tableExecutorService.shutdown();
        pendingMessages.clear();
        messagesToDisplay.clear();
    }

}


