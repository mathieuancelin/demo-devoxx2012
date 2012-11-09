package devoxx.plugin.translator;

import devoxx.api.NoteDoneEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import org.jboss.weld.environment.osgi.api.annotation.Specification;
import org.jboss.weld.environment.osgi.api.events.InterBundleEvent;

@ApplicationScoped
public class TweetPlugin {
    
    public void done(@Observes @Specification(NoteDoneEvent.class) InterBundleEvent evt) {
        NoteDoneEvent nde = (NoteDoneEvent) evt.get();
        try {
            Runtime.getRuntime().exec(new String[] {"/usr/local/bin/growlnotify", 
                "-n", "Nevernote", "-a", "Twitter.app", "-t", "Nevernote",  "-m", 
                "Task '" + nde.title + "' is done. \n\n'" 
                    + nde.content + "'.\n\nNice work ;-)"}).waitFor();
        } catch (Exception ex) {
           // ex.printStackTrace();
        }
        System.out.println("Tweeting that task : " + nde.title + " is done !!!");
    }
}
