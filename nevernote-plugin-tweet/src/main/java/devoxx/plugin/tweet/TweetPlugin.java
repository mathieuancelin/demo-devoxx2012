package devoxx.plugin.tweet;

import devoxx.api.NoteDoneEvent;
import devoxx.api.Plugin;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.jboss.weld.environment.osgi.api.annotation.Specification;
import org.jboss.weld.environment.osgi.api.events.InterBundleEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@ApplicationScoped
@Publish
public class TweetPlugin implements Plugin {
    
    @Inject BundleContext context;
    
    @Inject SystemTweetService service;
    
    public void done(@Observes @Specification(NoteDoneEvent.class) InterBundleEvent evt) {
        NoteDoneEvent nde = (NoteDoneEvent) evt.get();
        service.tweet(nde);
    }
    
    /***************************************/
    /** Plugin management related methods **/
    /***************************************/

    @Override
    public String pluginId() {
        return "tweet-plugin";
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
        return "Tweet plugin";
    }

    @Override
    public String desc() {
        return "A tweet plugin";
    }

    @Override
    public String icon() {
        return "icon-fire";
    }

    @Override
    public String apply(String content) {
        return content;
    }

    @Override
    public Map<String, File> resources() {
        return Collections.emptyMap();
    }

    @Override
    public boolean modifyContent() {
        return false;
    }
}
