package turbo;

import logger.Logger;
import logger.LoggerLevel;
import org.python.core.PyException;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TurboThreadExecutor {
    private final Logger logger;
    private final ThreadPoolExecutor executorService;

    public TurboThreadExecutor(Logger logger) {
        this.logger = logger;
        this.executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }

    public TurboThreadExecutor(Logger logger, int poolSize) {
        this.logger = logger;
        this.executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
    }

    public void submitTask(Runnable task) {
        executorService.submit(() -> {
            logger.logOutput(LoggerLevel.DEBUG, "Task started: " + task);
            try {
                task.run();
            } catch (PyException e) {
                logger.logOutput(LoggerLevel.UI,
                        "Jython code error. Please review.\n" + e.getMessage());
            } catch (Exception e) {
                logger.logOutput("Unknown exception: " + e.getMessage());
            } finally {
                logger.logOutput(LoggerLevel.DEBUG, "Task completed: " + task);
            }
        });
    }

    public void shutdown() {
        logger.logOutput(LoggerLevel.DEBUG, "Shutting down Executor service...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        logger.logOutput(LoggerLevel.DEBUG, "Executor Service shut down.");
        logger.logOutput(LoggerLevel.DEBUG, "Total tasks completed: " + executorService.getCompletedTaskCount());
        logger.logOutput(LoggerLevel.DEBUG, "Total active threads: " + executorService.getActiveCount());
    }
}
