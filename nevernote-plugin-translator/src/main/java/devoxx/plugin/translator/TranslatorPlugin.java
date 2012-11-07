package devoxx.plugin.translator;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import devoxx.api.Plugin;

@ApplicationScoped
public class TranslatorPlugin implements Plugin {
    
    public String pluginId() {
        return "french-translator";
    }
    
    public String name() {
        return "French translator";
    }
    
    public String desc() {
        return "A french translator"; 
    }
    
    public String apply(String content) {
        return "english";
    }
    
    public Map<String, File> resources() {
        return Collections.emptyMap();
    }
}
