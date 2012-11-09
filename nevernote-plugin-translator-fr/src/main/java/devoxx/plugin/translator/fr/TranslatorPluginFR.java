package devoxx.plugin.translator.fr;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import devoxx.api.*;
import java.io.IOException;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.jboss.weld.environment.osgi.api.events.BundleContainerEvents;
import org.osgi.framework.BundleContext;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

@Lang(Lang.Language.FR)
@Publish
@ApplicationScoped
public class TranslatorPluginFR implements Plugin {

    @Inject BundleContext context;
    
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
        String url = "https://www.googleapis.com/language/translate/v2?key=AIzaSyAE97NtcqqxxVFrvaHM39NyepZhvfHf1zk&target=fr&q=Hello%20world";
        try {
            JSONResource jsr = new Resty().json(url);
            String ret = (String) jsr.get("data.translations[0].translatedText");
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "french";
        }
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
