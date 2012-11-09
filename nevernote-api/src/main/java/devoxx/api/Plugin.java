package devoxx.api;

import org.osgi.framework.Bundle;

import java.io.File;
import java.util.Map;

public interface Plugin {
    
    public String pluginId();
    
    public Long bundleId();

    public Bundle bundle();
    
    public String name();
    
    public String desc();
    
    public String icon();
    
    public String apply(String content);
    
    public Map<String, File> resources();
    
    public boolean modifyContent();
    
}
