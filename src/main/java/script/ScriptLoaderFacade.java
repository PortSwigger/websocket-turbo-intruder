package script;

import config.ExtensionConfiguration;

import java.util.List;

public class ScriptLoaderFacade {
    private final ExtensionConfiguration extensionConfiguration;

    public ScriptLoaderFacade(ExtensionConfiguration extensionConfiguration) {
        this.extensionConfiguration = extensionConfiguration;
    }

    public List<Script> loadScripts() {
        ScriptLoader scriptLoader = extensionConfiguration.isDefault() ? new ClassPathScriptLoader() : new FilePathScriptLoader();

        return scriptLoader.loadScripts(extensionConfiguration.getWebSocketScriptPath());
    }
}
