package ui.editor.embedded;

import config.ExtensionConfiguration;
import embedded.ServerDetails;
import embedded.ServerStarter;
import script.MemoryScript;
import script.Script;
import script.ScriptLoaderFacade;
import ui.PanelSwitcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EmbeddedServerEditorController {
    private static final String DEFAULT_SERVER = """
            def create_connection(upgrade_request):
                connection = websocket_connection.create(upgrade_request, Engine.TURBO)
                return connection
            
            def handle_outgoing_message(websocket_message):
                results_table.add(websocket_message)
            
            def handle_incoming_message(websocket_message):
                results_table.add(websocket_message)""";

    private final ServerStarter serverStarter;
    private final ExtensionConfiguration extensionConfiguration;
    private final ScriptLoaderFacade scriptLoader;
    private final PanelSwitcher panelSwitcher;

    public EmbeddedServerEditorController(
            ServerStarter serverStarter,
            ExtensionConfiguration extensionConfiguration,
            ScriptLoaderFacade scriptLoader,
            PanelSwitcher panelSwitcher) {
        this.serverStarter = serverStarter;
        this.extensionConfiguration = extensionConfiguration;
        this.scriptLoader = scriptLoader;
        this.panelSwitcher = panelSwitcher;
    }

    public void startServer(ServerDetails serverDetails) {
        serverStarter.startServer(serverDetails);
        panelSwitcher.showAttackPanel();
    }

    public Script[] loadScripts(File file) {
        extensionConfiguration.setWebSocketScriptPath(file.getAbsolutePath());

        return loadScripts();
    }

    public Script[] loadScripts() {
        List<Script> scriptList = new ArrayList<>(scriptLoader.loadScripts());
        String script = extensionConfiguration.getEmbeddedServerScript();
        scriptList.addFirst(new MemoryScript(Objects.requireNonNullElse(script, DEFAULT_SERVER)));
        return scriptList.toArray(new Script[0]);
    }

    public void saveScript(String script) {
        extensionConfiguration.setEmbeddedServerScript(script);
    }

}
