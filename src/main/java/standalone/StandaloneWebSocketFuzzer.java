package standalone;

import attack.AttackManager;
import attack.AttackScriptExecutor;
import attack.AttackState;
import data.*;
import logger.Logger;
import logger.StandaloneLogging;
import python.ConnectionFactoryFactory;
import socket.TurboConfig;
import socket.TurboWebSocketMessagePublisher;
import socket.TurboWebSockets;

import java.io.IOException;
import java.util.Scanner;
import java.util.function.Consumer;

public class StandaloneWebSocketFuzzer {
    public static void main(String[] args) {
        StandaloneLogging logging = new StandaloneLogging();
        Logger logger = new Logger(logging);
        try {
            ParsedArgs parsedArgs = ParsedArgs.parse(args);
            logger.setDebugLogLevel(parsedArgs.debug());

            AttackState attackState = new AttackState();
            TurboWebSocketMessagePublisher publisher = new TurboWebSocketMessagePublisher();
            int availableCores = Runtime.getRuntime().availableProcessors() > 1 ? 2 : 1;
            TurboWebSockets webSockets = new TurboWebSockets(logger, new TurboConfig());

            MessagesToDisplay messagesToDisplay = new MessagesToDisplay(logger, attackState);
            PendingMessages pendingMessages = new PendingMessages(logger, attackState);

            AttackScriptExecutor scriptExecutor = new AttackScriptExecutor(
                    logger,
                    publisher,
                    messagesToDisplay,
                    new ConnectionFactoryFactory(
                            logger,
                            webSockets,
                            pendingMessages,
                            attackState::currentAttackId,
                            publisher
                    )
            );

            StandaloneWebSocketMessageModel model = new StandaloneWebSocketMessageModel(logger, parsedArgs.logFilePath());

            AttackManager attackManager = new AttackManager(
                    logger,
                    pendingMessages,
                    messagesToDisplay,
                    model::add,
                    scriptExecutor,
                    attackState
            );

            AttackDetails attackDetails = new AttackDetails(
                    availableCores,
                    parsedArgs.message(),
                    new TurboHttpRequest(parsedArgs.request(), parsedArgs.endpoint()),
                    parsedArgs.scriptCode()
            );

            attackManager.startAttack(attackDetails);
            Consumer<String> progress = new Consumer<String>() {
                @Override
                public void accept(String message) {
                    logger.raiseInfoEvent(message);
                }
            };
            attackManager.registerProgressListener(progress);

            Scanner s = new Scanner(System.in);

            System.out.println("Press enter to stop...");

            s.nextLine();

            logger.raiseInfoEvent("Exiting...");
            try {
                attackManager.stopAttack();

            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }


        } catch (IllegalArgumentException e) {
            logger.logError(e.getMessage());
            logger.logError("""
                    Usage: java -jar WebSocketFuzzer.jar <scriptFile> <baseRequestFile> <endpoint> <message>
                    Example: java -jar WebSocketFuzzer.jar resources/examples/basic.py resources/examples/request.txt https://example.net:443 foobar
                    """);
        } catch (IOException e) {
            logger.logError("File Error: " + e.getMessage());
        } catch (Exception e) {
            logger.logError("Unexpected Error: " + e.getMessage());
        }
        logger.raiseInfoEvent(" ");
    }
}
