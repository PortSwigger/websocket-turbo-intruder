package config;

import burp.api.montoya.persistence.Preferences;
import logger.Logger;
import logger.LoggerLevel;

import java.util.Objects;

public class ExtensionConfiguration {
    private static final String DEFAULT_SCRIPT_DIRECTORY = "/examples/";
    private static final String PREFERENCE_KEY_WEBSOCKET_SCRIPT = "websocketsScript";
    private static final String PREFERENCE_KEY_EMBEDDED_SERVER_SCRIPT = "embeddedServerScript";
    private static final String PREFERENCE_KEY_WEBSOCKET_SCRIPTS_PATH = "websocketsScriptsPath";
    private static final String PREFERENCE_KEY_WEBSOCKET_LOGGER = "websocketsLogger";

    private final Logger logger;
    private final Preferences preferences;

    public ExtensionConfiguration(Logger logger, Preferences preferences) {
        this.logger = logger;
        this.preferences = preferences;

        if (getWebSocketScriptPath() == null) {
            setWebSocketScriptPath();
        }
    }

    public String defaultDirectory() {
        return DEFAULT_SCRIPT_DIRECTORY;
    }

    public String getWebSocketScriptPath() {
        return preferences.getString(PREFERENCE_KEY_WEBSOCKET_SCRIPTS_PATH);
    }

    public void setWebSocketScriptPath(String scriptPath) {
        preferences.setString(PREFERENCE_KEY_WEBSOCKET_SCRIPTS_PATH, scriptPath);
        logger.logOutput(LoggerLevel.DEBUG, "Scripts directory set to " + scriptPath);
    }

    public String getWebSocketScript() {
        return preferences.getString(PREFERENCE_KEY_WEBSOCKET_SCRIPT);
    }

    public void setWebSocketScript(String script) {
        preferences.setString(PREFERENCE_KEY_WEBSOCKET_SCRIPT, script);
        logger.logOutput(LoggerLevel.DEBUG, "Script saved to the preferences.");
    }

    public Boolean getWebSocketsLogger() {
        return preferences.getBoolean(PREFERENCE_KEY_WEBSOCKET_LOGGER);
    }

    public void setWebSocketsLogger(boolean enabled) {
        preferences.setBoolean(PREFERENCE_KEY_WEBSOCKET_LOGGER, enabled);
    }

    public String getEmbeddedServerScript() {
        return preferences.getString(PREFERENCE_KEY_EMBEDDED_SERVER_SCRIPT);
    }

    public void setEmbeddedServerScript(String script) {
        preferences.setString(PREFERENCE_KEY_EMBEDDED_SERVER_SCRIPT, script);
        logger.logOutput(LoggerLevel.DEBUG, "Embedded Server Script saved to the preferences.");
    }

    public void setWebSocketScriptPath() {
        setWebSocketScriptPath(DEFAULT_SCRIPT_DIRECTORY);
    }

    public boolean isDefault() {
        return Objects.equals(getWebSocketScriptPath(), DEFAULT_SCRIPT_DIRECTORY);
    }
}
