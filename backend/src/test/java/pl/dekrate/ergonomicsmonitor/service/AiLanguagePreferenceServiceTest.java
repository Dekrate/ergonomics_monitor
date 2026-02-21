package pl.dekrate.ergonomicsmonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.dekrate.ergonomicsmonitor.model.AiLanguage;

@DisplayName("AI Language Preference Service")
class AiLanguagePreferenceServiceTest {

    @Test
    @DisplayName("should initialize with configured language and update at runtime")
    void shouldInitializeAndUpdateLanguage() {
        AiLanguagePreferenceService service = new AiLanguagePreferenceService("PL");

        assertEquals(AiLanguage.PL, service.getCurrentLanguage());

        AiLanguage updated = service.setCurrentLanguage(AiLanguage.EN);
        assertEquals(AiLanguage.EN, updated);
        assertEquals(AiLanguage.EN, service.getCurrentLanguage());
    }

    @Test
    @DisplayName("should fallback to EN when configured value is invalid")
    void shouldFallbackToEnglishForInvalidValue() {
        AiLanguagePreferenceService service = new AiLanguagePreferenceService("not-a-language");

        assertEquals(AiLanguage.EN, service.getCurrentLanguage());
        assertTrue(service.getSupportedLanguages().contains(AiLanguage.PL));
        assertTrue(service.getSupportedLanguages().contains(AiLanguage.EN));
    }
}
