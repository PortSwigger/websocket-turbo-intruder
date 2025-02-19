package attack;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.Direction;
import connection.Connection;
import data.ConnectionMessage;
import data.FrameType;
import data.MessagesToDisplay;
import data.WebSocketAttributeType;
import interpreter.Interpreter;
import logger.Logger;
import logger.LoggerLevel;
import python.ConnectionFactoryFactory;
import python.ResultsTable;
import socket.TurboWebSocketMessagePublisher;
import turbo.TurboThreadExecutor;
import utils.Utilities;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;

import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;
import static burp.api.montoya.websocket.Direction.SERVER_TO_CLIENT;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class AttackScriptExecutor {
    private final Logger logger;
    private final MessagesToDisplay messagesToDisplay;
    private final ConnectionFactoryFactory connectionFactoryFactory;
    private final TurboWebSocketMessagePublisher publisher;
    private TurboThreadExecutor threadExecutor;

    private Interpreter interpreter;

    public AttackScriptExecutor(
            Logger logger,
            TurboWebSocketMessagePublisher publisher,
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
        threadExecutor.submitTask(()-> interpreter.execute("queue_websockets(upgrade_request, message)"));
    }

    public void processMessage(ConnectionMessage webSocketConnectionMessage) {
        String messageParameterName = "websocket_message";
        String interestingParameterName = "interesting";
        interpreter.setVariable(messageParameterName, new DecoratedConnectionMessage(webSocketConnectionMessage));
        interpreter.setVariable(interestingParameterName, webSocketConnectionMessage.isInteresting());

        String callbackMethod = webSocketConnectionMessage.getDirection() == CLIENT_TO_SERVER
                ? "handle_outgoing_message"
                : "handle_incoming_message";
        interpreter.execute(String.format("%s(%s,%s)", callbackMethod, messageParameterName, interestingParameterName));
//            interpreter.execute(String.format("%s(%s)", callbackMethod, messageParameterName));
    }

    public void stopAttack() {
        if (publisher != null) publisher.close();
        if (interpreter != null) interpreter.close();
        threadExecutor.shutdown();
    }

    private static class DecoratedConnectionMessage implements ConnectionMessage {
        private final ConnectionMessage webSocketConnectionMessage;

        DecoratedConnectionMessage(ConnectionMessage webSocketConnectionMessage) {
            this.webSocketConnectionMessage = webSocketConnectionMessage;
        }

        @Override
        public String getMessage() {
            return webSocketConnectionMessage.getMessage();
        }

        @Override
        public Direction getDirection() {
            return webSocketConnectionMessage.getDirection();
        }

        @Override
        public long getPayloadLength() {
            return webSocketConnectionMessage.getPayloadLength();
        }

        @Override
        public int getMessageLength() {
            return webSocketConnectionMessage.getMessageLength();
        }

        @Override
        public LocalDateTime getDateTime() {
            return webSocketConnectionMessage.getDateTime();
        }

        @Override
        public long getDateTimeInMilliseconds() {
            return webSocketConnectionMessage.getDateTimeInMilliseconds();
        }

        @Override
        public void setTime(long time) {
        }

        @Override
        public long getTime() {
            return webSocketConnectionMessage.getTime();
        }

        @Override
        public String getComment() {
            return webSocketConnectionMessage.getComment();
        }

        @Override
        public void setComment(String comment) {
            webSocketConnectionMessage.setComment(comment);
        }

        @Override
        public Connection getConnection() {
            return webSocketConnectionMessage.getConnection();
        }

        @Override
        public void send() {
        }

        @Override
        public long getId() {
            return webSocketConnectionMessage.getId();
        }

        @Override
        public long getHash() {
            return webSocketConnectionMessage.getHash();
        }

        @Override
        public void setHash(long id) {
            webSocketConnectionMessage.setHash(id);
        }

        @Override
        public int getCode() {
            return FrameType.TEXT.value;
        }

        @Override
        public void setAttributes(Map<WebSocketAttributeType, Integer> attributes) {
            webSocketConnectionMessage.setAttributes(attributes);
        }

        @Override
        public int getWordCount() {
            return webSocketConnectionMessage.getWordCount();
        }

        @Override
        public int getKeyCount() {
            return webSocketConnectionMessage.getKeyCount();
        }

        @Override
        public boolean isInteresting() {
            return webSocketConnectionMessage.isInteresting();
        }

        @Override
        public void setInteresting(boolean interesting) {
            webSocketConnectionMessage.setInteresting(interesting);
        }

        @Override
        public ByteBuffer getMessageByteBuffer() {
            return webSocketConnectionMessage.getMessageByteBuffer();
        }

    }
}
