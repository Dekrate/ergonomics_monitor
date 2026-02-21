package pl.dekrate.ergonomicsmonitor.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.model.AiLanguage;

/**
 * Stores runtime AI language preference for dashboard and AI analysis features.
 *
 * <p>Preference is kept in-memory for now and can be changed at runtime via REST API.
 */
@Service
public class AiLanguagePreferenceService {

    private final AtomicReference<AiLanguage> currentLanguage;

    public AiLanguagePreferenceService(
            @Value("${ergonomics.ai.default-language:EN}") String defaultLanguageRaw) {
        this.currentLanguage = new AtomicReference<>(parseLanguage(defaultLanguageRaw));
    }

    /**
     * Returns current AI language mode.
     *
     * @return current language
     */
    public AiLanguage getCurrentLanguage() {
        return currentLanguage.get();
    }

    /**
     * Updates current AI language mode.
     *
     * @param language new language
     * @return language after update
     */
    public AiLanguage setCurrentLanguage(AiLanguage language) {
        currentLanguage.set(language);
        return language;
    }

    /**
     * Lists all supported AI language modes.
     *
     * @return immutable list of supported modes
     */
    public List<AiLanguage> getSupportedLanguages() {
        return Arrays.asList(AiLanguage.values());
    }

    private AiLanguage parseLanguage(String value) {
        try {
            return AiLanguage.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException _) {
            return AiLanguage.EN;
        }
    }
}
