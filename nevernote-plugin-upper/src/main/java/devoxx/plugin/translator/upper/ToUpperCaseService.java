package devoxx.plugin.translator.upper;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ToUpperCaseService {
    
    public String toUpperCase(String value) {
        return value.toUpperCase();
    }
}
