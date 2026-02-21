package pl.dekrate.ergonomicsmonitor.controller;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dekrate.ergonomicsmonitor.model.AiLanguage;
import pl.dekrate.ergonomicsmonitor.service.AiAssistantService;
import pl.dekrate.ergonomicsmonitor.service.AiLanguagePreferenceService;
import reactor.core.publisher.Mono;

/**
 * REST API for AI runtime settings used by the frontend dashboard.
 *
 * <p>Current scope:
 *
 * <ul>
 *   <li>Read current AI language mode.
 *   <li>Update language mode (PL/EN).
 * </ul>
 */
@RestController
@RequestMapping("/api/ai")
public class AiSettingsController {

    private final AiLanguagePreferenceService languagePreferenceService;
    private final AiAssistantService aiAssistantService;

    public AiSettingsController(
            AiLanguagePreferenceService languagePreferenceService,
            AiAssistantService aiAssistantService) {
        this.languagePreferenceService = languagePreferenceService;
        this.aiAssistantService = aiAssistantService;
    }

    /**
     * Gets current AI settings.
     *
     * @return current language and supported language list
     */
    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getSettings() {
        return Mono.just(
                Map.of(
                        "language", languagePreferenceService.getCurrentLanguage(),
                        "supportedLanguages", languagePreferenceService.getSupportedLanguages(),
                        "updatedAt", Instant.now()));
    }

    /**
     * Updates AI language mode.
     *
     * @param request update request payload
     * @return updated settings snapshot
     */
    @PatchMapping(value = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> updateSettings(@RequestBody AiSettingsUpdateRequest request) {
        AiLanguage language = request.language();
        if (language == null) {
            return Mono.error(new IllegalArgumentException("language cannot be null"));
        }

        AiLanguage updated = languagePreferenceService.setCurrentLanguage(language);
        return Mono.just(
                Map.of(
                        "language", updated,
                        "supportedLanguages", languagePreferenceService.getSupportedLanguages(),
                        "updatedAt", Instant.now()));
    }

    /**
     * Asks AI a free-form question within the context of recent user activity.
     *
     * @param request question payload
     * @return answer and metadata
     */
    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> askAi(@RequestBody AskAiRequest request) {
        if (request.userId() == null) {
            return Mono.error(new IllegalArgumentException("userId cannot be null"));
        }
        if (request.question() == null || request.question().isBlank()) {
            return Mono.error(new IllegalArgumentException("question cannot be blank"));
        }
        if (request.question().length() > 800) {
            return Mono.error(new IllegalArgumentException("question is too long"));
        }

        return aiAssistantService.askQuestion(request.userId(), request.question().trim());
    }

    /** DTO for AI settings update requests. */
    public record AiSettingsUpdateRequest(AiLanguage language) {}

    /** DTO for contextual AI question requests. */
    public record AskAiRequest(UUID userId, String question) {}
}
