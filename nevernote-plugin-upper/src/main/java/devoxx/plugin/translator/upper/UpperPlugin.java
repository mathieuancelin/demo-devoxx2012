package devoxx.plugin.translator.upper;

import devoxx.api.Plugin;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.weld.environment.osgi.api.annotation.BundleDataFile;
import org.jboss.weld.environment.osgi.api.annotation.BundleHeader;
import org.jboss.weld.environment.osgi.api.annotation.BundleHeaders;
import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@Publish
@ApplicationScoped
public class UpperPlugin implements Plugin {
    
    @Inject BundleContext context;
    
    @Inject Bundle bundle;
    
    @Inject @BundleHeaders Map<String, String> headers;
    
    @Inject @BundleHeader("Bundle-SymbolicName") String symbolicName;
    
    @Inject @BundleDataFile("dictionnary.txt") File text;

    @Inject ToUpperCaseService service;
    
    public String apply(String content) {
        return service.toUpperCase(content);
    }
        
    /***************************************/
    /** Plugin management related methods **/
    /***************************************/
    
    @Override
    public String pluginId() {
        return "upper-plugin";
    }

    @Override
    public Long bundleId() {
        return context.getBundle().getBundleId();
    }

    @Override
    public Bundle bundle() {
        return context.getBundle();
    }

    @Override
    public String name() {
        return "Upper plugin";
    }

    @Override
    public String desc() {
        return "A upper plugin";
    }

    @Override
    public String icon() {
        return "icon-arrow-up";
    }

    @Override
    public Map<String, File> resources() {
        return Collections.emptyMap();
    }

    @Override
    public boolean modifyContent() {
        return true;
    }
}
