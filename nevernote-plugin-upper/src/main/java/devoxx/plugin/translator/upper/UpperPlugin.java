package devoxx.plugin.translator.upper;

import devoxx.api.Plugin;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.weld.environment.osgi.api.annotation.Publish;
import org.osgi.framework.BundleContext;

@ApplicationScoped
@Publish
public class UpperPlugin implements Plugin {
    
    @Inject BundleContext context;

    @Override
    public String pluginId() {
        return "upper-plugin";
    }

    @Override
    public Long bundleId() {
        return context.getBundle().getBundleId();
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
        return "icon-fire";
    }

    @Override
    public String apply(String content) {
        return content.toUpperCase();
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
