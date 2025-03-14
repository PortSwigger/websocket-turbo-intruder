package ui.editor;

import burp.api.montoya.ai.Ai;
import burp.api.montoya.ai.chat.Message;
import burp.api.montoya.ai.chat.PromptException;
import burp.api.montoya.ai.chat.PromptOptions;
import burp.api.montoya.ai.chat.PromptResponse;
import logger.Logger;
import turbo.TurboThreadExecutor;
import utils.AiUtilities;

import javax.swing.*;
import javax.swing.text.JTextComponent;

public class AiManager {
    private final TurboThreadExecutor threadExecutor;
    private final Logger logger;
    private final Ai ai;

    public AiManager(Logger logger, Ai ai) {
        this.ai = ai;
        this.logger = logger;
        this.threadExecutor = new TurboThreadExecutor(logger);
    }

    public boolean isEnabled() {
        return ai != null && ai.isEnabled();
    }

    public void prompt(JButton sendButton, JTextArea inputArea,
                       JTextComponent scriptTextComponent) {
        if (ai != null && ai.isEnabled()) {
            threadExecutor.submitTask(() -> {
                sendButton.setText("Generating...");
                sendButton.setEnabled(false);
                scriptTextComponent.setEnabled(false);
                try {
                    PromptResponse response = ai.prompt().execute(
                            PromptOptions.promptOptions(),
                            Message.systemMessage(AiUtilities.SYSTEM_PROMPT),
                            Message.userMessage(inputArea.getText()));
                    scriptTextComponent.setText(response.content());
                } catch (PromptException e) {
                    if (e.getMessage().contains("Not enough credits")) {
                        logger.logError("Please increase your credit balance.");
                    } else {
                        logger.logError("Issue executing prompt: " + e.getMessage());
                    }
                }
                scriptTextComponent.setEnabled(true);
                sendButton.setEnabled(true);
                sendButton.setText("Ask AI");
            });
        }
    }

    public void stop() {
        threadExecutor.shutdown();
    }
}
