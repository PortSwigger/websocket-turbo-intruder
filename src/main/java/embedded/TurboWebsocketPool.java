package embedded;

import attack.AttackState;
import logger.Logger;
import logger.StandaloneLogging;
import socket.TurboConfig;
import socket.TurboWebSocketMessagePublisher;
import socket.TurboWebSockets;


public class TurboWebsocketPool {
    public TurboWebsocketPool() {
        StandaloneLogging logging = new StandaloneLogging();
        Logger logger = new Logger(logging);
        try {
            logger.setDebugLogLevel(true);

            AttackState attackState = new AttackState();
            TurboWebSocketMessagePublisher publisher = new TurboWebSocketMessagePublisher();
            int availableCores = Runtime.getRuntime().availableProcessors() > 1 ? 2 : 1;
            TurboWebSockets webSockets = new TurboWebSockets(logger, new TurboConfig());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
