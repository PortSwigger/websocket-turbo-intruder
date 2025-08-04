package attack;

import burp.api.montoya.http.message.requests.HttpRequest;
import data.ConnectionMessage;
import data.DecoratedConnectionMessage;
import data.MessagesToDisplay;
import interpreter.Interpreter;
import logger.Logger;
import logger.LoggerLevel;
import python.ConnectionFactoryFactory;
import python.ResultsTable;
import socket.TurboConnectionPublisher;
import turbo.TurboThreadExecutor;
import utils.Utilities;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;

public class AttackScriptExecutor {
    private final Logger logger;
    private final MessagesToDisplay messagesToDisplay;
    private final ConnectionFactoryFactory connectionFactoryFactory;
    private final TurboConnectionPublisher publisher;
    private TurboThreadExecutor threadExecutor;

    private Interpreter interpreter;

    public AttackScriptExecutor(
            Logger logger,
            TurboConnectionPublisher publisher,
            MessagesToDisplay messagesToDisplay,
            ConnectionFactoryFactory connectionFactoryFactory
    ) {
        this.logger = logger;
        this.messagesToDisplay = messagesToDisplay;
        this.connectionFactoryFactory = connectionFactoryFactory;
        this.publisher = publisher;
    }

    public void startAttack(String message, HttpRequest upgradeRequest, String editorCodeString) {
        try {
            String environment = Utilities.loadScriptEnvironment();
            interpreter = new Interpreter(logger);
            interpreter.setVariable("websocket_connection", connectionFactoryFactory.create());
            interpreter.setVariable("results_table", new ResultsTable(messagesToDisplay));

            interpreter.setVariable("message", message);
            interpreter.setVariable("upgrade_request", upgradeRequest);

            interpreter.execute(environment);
            interpreter.execute(editorCodeString);
        } catch (Exception e) {
            logger.logOutput(LoggerLevel.UI, "Jython code error. Please review.\r\n" + e);

            interpreter.close();

            throw new IllegalArgumentException(e);
        }
        threadExecutor = new TurboThreadExecutor(logger);
        threadExecutor.submitTask(() -> interpreter.execute("queue_websockets(upgrade_request, message)"));
    }

    public void processMessage(ConnectionMessage webSocketConnectionMessage) {
        String messageParameterName = "websocket_message";
        interpreter.setVariable(messageParameterName, new DecoratedConnectionMessage(webSocketConnectionMessage));

        String callbackMethod = webSocketConnectionMessage.getDirection() == CLIENT_TO_SERVER
                ? "handle_outgoing_message"
                : "handle_incoming_message";
        interpreter.execute(String.format("%s(%s)", callbackMethod, messageParameterName));
    }

    public void stopAttack() {
        if (publisher != null) publisher.close();
        if (interpreter != null) interpreter.close();
        if (threadExecutor != null) threadExecutor.shutdown();
    }

}
