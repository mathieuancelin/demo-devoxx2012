package devoxx.plugin.translator.en;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import devoxx.api.*;
import devoxx.core.fwk.Constants;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.jboss.weld.environment.osgi.api.events.BundleContainerEvents;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@Lang(Lang.Language.EN)
@Publish
@ApplicationScoped
public class TranslatorPluginEN implements Plugin {
    
    @Inject BundleContext context;
    
    @Inject TranslatorService service;
    
    public String apply(String content) {
        return service.translate(content, Language.ENGLISH);
    }
    
    /***************************************/
    /** Plugin management related methods **/
    /***************************************/
    
    public String pluginId() {
        return "english-translator";
    }
    
    public String name() {
        return "English translator";
    }
    
    public String desc() {
        return "A english translator"; 
    }
    
    public Map<String, File> resources() {
        return Collections.emptyMap();
    }
    
    public void start(@Observes BundleContainerEvents.BundleContainerInitialized evt) {
        devoxx.core.fwk.SimpleLogger.info("Starting ...");
    }
    
    @Override
    public String icon() {
        return "icon-comment";
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
    public boolean modifyContent() {
        return true;
    }
}
