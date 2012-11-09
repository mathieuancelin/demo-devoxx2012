package devoxx.plugin.translator;

import devoxx.api.NoteDoneEvent;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import devoxx.api.Plugin;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.jboss.weld.environment.osgi.api.annotation.Specification;
import org.jboss.weld.environment.osgi.api.events.BundleContainerEvents;
import org.jboss.weld.environment.osgi.api.events.InterBundleEvent;
import org.osgi.framework.BundleContext;

@ApplicationScoped
@Publish
public class TranslatorPlugin implements Plugin {

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
        return "english";
    }
    
    public Map<String, File> resources() {
        return Collections.emptyMap();
    }
    
    public void start(@Observes BundleContainerEvents.BundleContainerInitialized evt) {
        System.out.println("Yeah bro !!!");
    }
    
    public void done(@Observes @Specification(NoteDoneEvent.class) InterBundleEvent evt) {
        NoteDoneEvent nde = (NoteDoneEvent) evt.get();
        System.out.println("Tweeting that task : " + nde.title + " is done !!!");
    }

    @Override
    public String icon() {
        return "icon-ok";
    }
}
