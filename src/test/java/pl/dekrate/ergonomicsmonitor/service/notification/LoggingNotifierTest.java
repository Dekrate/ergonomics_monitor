package pl.dekrate.ergonomicsmonitor.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.dekrate.ergonomicsmonitor.model.ActivityIntensityMetrics;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for LoggingNotifier.
 *
 * @author dekrate
 * @version 1.0
 */
@DisplayName("LoggingNotifier")
class LoggingNotifierTest {

    private LoggingNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new LoggingNotifier();
    }

    @Test
    @DisplayName("should complete successfully when sending notification")
    void shouldCompleteSuccessfully() {
        // given
        BreakRecommendation recommendation = createRecommendation(BreakUrgency.MEDIUM);

        // when
        Mono<Void> result = notifier.sendNotification(recommendation);

        // then
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("should handle all urgency levels")
    void shouldHandleAllUrgencyLevels() {
        // given
        for (BreakUrgency urgency : BreakUrgency.values()) {
            BreakRecommendation recommendation = createRecommendation(urgency);

            // when
            Mono<Void> result = notifier.sendNotification(recommendation);

            // then
            StepVerifier.create(result)
                    .expectComplete()
                    .verify();
        }
    }

    @Test
    @DisplayName("should return correct notifier type")
    void shouldReturnCorrectType() {
        // when
        String type = notifier.getNotifierType();

        // then
        assertEquals("Logging Only", type);
    }

    private BreakRecommendation createRecommendation(BreakUrgency urgency) {
        return BreakRecommendation.builder()
                .timestamp(Instant.now())
                .urgency(urgency)
                .reason("Test recommendation")
                .suggestedBreakDuration(Duration.ofMinutes(5))
                .metrics(ActivityIntensityMetrics.builder()
                        .totalEvents(100)
                        .keyboardEvents(60)
                        .mouseEvents(40)
                        .timeWindow(Duration.ofMinutes(10))
                        .build())
                .build();
    }
}

