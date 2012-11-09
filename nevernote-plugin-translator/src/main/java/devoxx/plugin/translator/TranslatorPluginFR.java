package devoxx.plugin.translator;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import devoxx.api.*;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.jboss.weld.environment.osgi.api.events.BundleContainerEvents;
import org.jboss.weld.environment.osgi.api.events.InterBundleEvent;
import org.osgi.framework.BundleContext;

@Lang(Lang.Language.FR)
@Publish
@ApplicationScoped
public class TranslatorPluginFR implements Plugin {

    @Inject
    BundleContext context;
    
    public Long pluginId() {
        return context.getBundle().getBundleId();
    }
    
    public String name() {
        return context.getBundle().getSymbolicName() + ": French translator";
    }
    
    public String desc() {
        return "A french translator"; 
    }
    
    public String apply(String content) {
        String url = "https://www.googleapis.com/language/translate/v2?key=AIzaSyCbAOjY3ODiL1aB8kdXsEkcBJblX48fS5U&source=fr&target=de&q=Hello%20world";
        return "french";
    }
    
    public Map<String, File> resources() {
        return Collections.emptyMap();
    }
    
    public void start(@Observes BundleContainerEvents.BundleContainerInitialized evt) {
        System.out.println("Yeah bro !!!");
    }
    

    @Override
    public String icon() {
        return "icon-comment";
    }
}
