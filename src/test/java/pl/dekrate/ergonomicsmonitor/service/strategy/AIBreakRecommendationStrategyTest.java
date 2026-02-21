package pl.dekrate.ergonomicsmonitor.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Break Recommendation Strategy Tests")
class AIBreakRecommendationStrategyTest {

    @Mock private ChatClient chatClient;

    private AIBreakRecommendationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AIBreakRecommendationStrategy(chatClient);
    }

    @Test
    @DisplayName("Should return empty Mono when events list is null or empty")
    void shouldReturnEmptyMonoWhenEventsListIsNullOrEmpty() {
        // When & Then
        StepVerifier.create(strategy.analyze(null)).verifyComplete();

        StepVerifier.create(strategy.analyze(Collections.emptyList())).verifyComplete();
    }

    @Nested
    @DisplayName("AI Query Tests")
    class AiQueryTests {

        @Test
        @DisplayName("Should return recommendation when AI suggests a break")
        void shouldReturnRecommendationWhenAiSuggestsBreak() {
            // Given
            List<ActivityEvent> events = List.of(createEvent(150.0), createEvent(180.0));

            String aiJsonResponse =
                    """
                    {
                        "needsBreak": true,
                        "urgency": "CRITICAL",
                        "durationMinutes": 15,
                        "reason": "Very high intensity detected"
                    }
                    """;

            mockChatClientCall(aiJsonResponse);

            // When & Then
            StepVerifier.create(strategy.analyze(events))
                    .assertNext(
                            recommendation -> {
                                assertThat(recommendation.getUrgency())
                                        .isEqualTo(BreakUrgency.CRITICAL);
                                assertThat(recommendation.getDurationMinutes()).isEqualTo(15);
                                assertThat(recommendation.getReason())
                                        .contains("Very high intensity detected");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty Mono when AI says no break needed")
        void shouldReturnEmptyMonoWhenAiSaysNoBreakNeeded() {
            // Given
            List<ActivityEvent> events = List.of(createEvent(50.0));
            String aiJsonResponse =
                    """
                    {
                        "needsBreak": false
                    }
                    """;

            mockChatClientCall(aiJsonResponse);

            // When & Then
            StepVerifier.create(strategy.analyze(events)).verifyComplete();
        }

        @Test
        @DisplayName("Should use fallback when AI response is incomplete")
        void shouldUseFallbackWhenAiResponseIsIncomplete() {
            // Given
            List<ActivityEvent> events = List.of(createEvent(200.0), createEvent(220.0));
            // Contains needsBreak: true but missing urgency and others
            String incompleteResponse = "{ \"needsBreak\": true }";

            mockChatClientCall(incompleteResponse);

            // When & Then
            StepVerifier.create(strategy.analyze(events))
                    .assertNext(
                            recommendation -> {
                                assertThat(recommendation.getReason())
                                        .contains("fallback analysis");
                                assertThat(recommendation.getUrgency())
                                        .isEqualTo(BreakUrgency.CRITICAL);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should use fallback when AI service fails")
        void shouldUseFallbackWhenAiServiceFails() {
            // Given
            List<ActivityEvent> events = List.of(createEvent(100.0));

            ChatClient.ChatClientRequestSpec requestSpec =
                    mock(ChatClient.ChatClientRequestSpec.class);
            when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
            when(requestSpec.call()).thenThrow(new RuntimeException("AI Service Down"));

            // When & Then
            StepVerifier.create(strategy.analyze(events))
                    .assertNext(
                            recommendation -> {
                                assertThat(recommendation.getReason())
                                        .contains("fallback analysis");
                                assertThat(recommendation.getUrgency())
                                        .isEqualTo(BreakUrgency.MEDIUM);
                            })
                    .verifyComplete();
        }
    }

    private void mockChatClientCall(String response) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(response);
    }

    private ActivityEvent createEvent(double intensity) {
        return ActivityEvent.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .timestamp(Instant.now())
                .type(ActivityType.KEYBOARD)
                .intensity(intensity)
                .build();
    }
}
