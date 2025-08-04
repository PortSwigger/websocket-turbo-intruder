package ui.editor;

import attack.AttackStarter;
import config.ExtensionConfiguration;
import data.AttackDetails;
import script.MemoryScript;
import script.Script;
import script.ScriptLoaderFacade;
import ui.PanelSwitcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WebSocketEditorController {
    private final AttackStarter attackStarter;
    private final PanelSwitcher panelSwitcher;
    private final ExtensionConfiguration extensionConfiguration;
    private final ScriptLoaderFacade scriptLoader;

    public WebSocketEditorController(AttackStarter attackStarter, PanelSwitcher panelSwitcher, ExtensionConfiguration extensionConfiguration, ScriptLoaderFacade scriptLoader) {
        this.attackStarter = attackStarter;
        this.panelSwitcher = panelSwitcher;
        this.extensionConfiguration = extensionConfiguration;
        this.scriptLoader = scriptLoader;
    }

    public void startAttack(AttackDetails attackDetails) {
        attackStarter.startAttack(attackDetails);

        panelSwitcher.showAttackPanel();
    }

    public Script[] loadScripts(File file) {
        extensionConfiguration.setWebSocketScriptPath(file.getAbsolutePath());

        return loadScripts();
    }

    public Script[] loadScripts() {
        List<Script> scriptList = new ArrayList<>(scriptLoader.loadScripts());
        String script = extensionConfiguration.getWebSocketScript();
        if (script != null) scriptList.addFirst(new MemoryScript(script));
        return scriptList.toArray(new Script[0]);
    }

    public void saveScript(String script) {
        extensionConfiguration.setWebSocketScript(script);
    }

}
