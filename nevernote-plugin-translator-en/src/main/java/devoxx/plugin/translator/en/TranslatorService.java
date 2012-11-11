package devoxx.plugin.translator.en;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import devoxx.core.fwk.Constants;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TranslatorService {
    
    public String translate(String content, Language lang) {
        Translate.setClientId(Constants.clientId);
        Translate.setClientSecret(Constants.clientSecret);
        try {
            String translatedText = Translate.execute(content, lang);
            return translatedText;
        } catch (Exception ex) {
            return lang.toString();
        }
    }
}
