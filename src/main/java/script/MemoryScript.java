package script;

import java.nio.file.Path;

public class MemoryScript implements Script {
    private static final String NAME = "last used";
    private final String content;

    public MemoryScript(String content) {
        this.content = content;
    }

    @Override
    public String content() {
        return content;
    }
    @Override
    public String toString() {
        return NAME;
    }
}
