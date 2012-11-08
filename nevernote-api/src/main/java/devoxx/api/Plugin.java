package devoxx.api;

import java.io.File;
import java.util.Map;

public interface Plugin {
    
    public String pluginId();
    
    public String name();
    
    public String desc();
    
    public String icon();
    
    public String apply(String content);
    
    public Map<String, File> resources();
    
}
