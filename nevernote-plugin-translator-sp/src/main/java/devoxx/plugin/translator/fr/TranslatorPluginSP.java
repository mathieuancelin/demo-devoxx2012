package devoxx.plugin.translator.fr;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import devoxx.api.*;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.jboss.weld.environment.osgi.api.events.BundleContainerEvents;
import org.osgi.framework.BundleContext;

@Lang(Lang.Language.SP)
@Publish
@ApplicationScoped
public class TranslatorPluginSP implements Plugin {
    
    @Inject BundleContext context;

    public String pluginId() {
        return "spanish-translator";
    }
    
    public String name() {
        return "Spanish translator";
    }
    
    public String desc() {
        return "A spanish translator"; 
    }
    
    public String apply(String content) {
        String url = "https://www.googleapis.com/language/translate/v2?key=AIzaSyCbAOjY3ODiL1aB8kdXsEkcBJblX48fS5U&source=sp&target=de&q=Hello%20world";
        return "spanish";
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
    
    public Long bundleId() {
        return context.getBundle().getBundleId();
    }
    
    @Override
    public boolean modifyContent() {
        return true;
    }
}
