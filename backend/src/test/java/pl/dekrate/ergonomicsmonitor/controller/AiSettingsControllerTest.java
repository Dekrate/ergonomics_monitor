package pl.dekrate.ergonomicsmonitor.controller;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import pl.dekrate.ergonomicsmonitor.service.AiAssistantService;
import pl.dekrate.ergonomicsmonitor.service.AiLanguagePreferenceService;

@DisplayName("AI Settings Controller")
class AiSettingsControllerTest {

    private WebTestClient buildClient() {
        AiLanguagePreferenceService preferenceService = new AiLanguagePreferenceService("EN");
        AiAssistantService assistantService =
                new AiAssistantService(
                        mock(ActivityRepository.class), mock(ChatClient.class), preferenceService);
        AiSettingsController controller =
                new AiSettingsController(preferenceService, assistantService);
        return WebTestClient.bindToController(controller).build();
    }

    @Test
    @DisplayName("should return current AI settings")
    void shouldReturnCurrentSettings() {
        buildClient()
                .get()
                .uri("/api/ai/settings")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.language")
                .isEqualTo("EN")
                .jsonPath("$.supportedLanguages[0]")
                .exists();
    }

    @Test
    @DisplayName("should update AI language mode")
    void shouldUpdateLanguageMode() {
        buildClient()
                .patch()
                .uri("/api/ai/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"language\":\"PL\"}")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.language")
                .isEqualTo("PL");
    }
}
