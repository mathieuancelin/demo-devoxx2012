package devoxx.plugin.translator.upper;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ToLowerCaseService {
    
    public String toUpperCase(String value) {
        return value.toLowerCase();
    }
}
