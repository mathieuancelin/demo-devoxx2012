package devoxx.plugin.tweet;

import devoxx.api.NoteDoneEvent;
import devoxx.core.fwk.SimpleLogger;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SystemTweetService {
    
    public void tweet(NoteDoneEvent nde) {
        try {
            Runtime.getRuntime().exec(new String[] {"/usr/local/bin/growlnotify", 
                "-n", "Nevernote", "-a", "Twitter.app", "-t", "Twitter",  "-m", 
                "Task '" + nde.title + "' is done. \n\n'" 
                    + nde.content + "'.\n\nNice work ;-)"}).waitFor();
        } catch (Exception ex) {
           // ex.printStackTrace();
        }
        SimpleLogger.info("Tweeting that task : {} is done !!!", nde.title);
    }
}
