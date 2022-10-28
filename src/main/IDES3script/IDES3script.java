package IDES3script;

import ides.api.plugin.Plugin;
import ides.api.plugin.operation.OperationManager;

public class IDES3script implements Plugin {
    public static final ClassLoader loader = IDES3script.class.getClassLoader();
    private ExecuteScript es;

    public String getCredits() {
        return "Yunshan (Richard) Yan";
    }
    public String getDescription() {
        return "IDES 3 Scripting Plugin\n" +
               "Operations available: [\n" +
                ExecuteScript.refreshOperationMapping(es.engine()) +
               "\n]";
    }
    public String getLicense() {
        return "";
    }
    public String getName() {
        return "IDES 3 Scripting Plugin";
    }
    public String getVersion() {
        return "v0.2.1";
    }

    public void initialize() {
        es = new ExecuteScript();
        es.init();
        OperationManager.instance().register(es);
    }
    public void unload() {
    }
}