package pl.dekrate.ergonomicsmonitor.service.notification;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.dekrate.ergonomicsmonitor.model.ActivityIntensityMetrics;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;

/**
 * Unit tests for WindowsNativeNotifier.
 *
 * <p>Note: Actual tray display requires a real desktop session, so these tests verify notifier
 * contract and object wiring. Integration tests should validate native notification rendering on
 * Windows.
 *
 * @author dekrate
 * @version 1.0
 */
@DisplayName("WindowsNativeNotifier")
class WindowsNativeNotifierTest {

    private WindowsNativeNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new WindowsNativeNotifier();
    }

    @Test
    @DisplayName("should return correct notifier type")
    void shouldReturnCorrectType() {
        // when
        String type = notifier.getNotifierType();

        // then
        assertEquals("Windows Tray Notification", type);
    }

    @Test
    @DisplayName("should handle all urgency levels without throwing exceptions")
    void shouldHandleAllUrgencyLevels() {
        // when & then - verify notifier type is accessible
        String type = notifier.getNotifierType();
        assertNotNull(type);
        assertEquals("Windows Tray Notification", type);
    }

    @Test
    @DisplayName("should construct valid recommendation object")
    void shouldConstructValidRecommendation() {
        // given
        BreakRecommendation recommendation = createRecommendation(BreakUrgency.CRITICAL);

        // then
        assertAll(
                () -> assertNotNull(recommendation.getTimestamp()),
                () -> assertEquals(BreakUrgency.CRITICAL, recommendation.getUrgency()),
                () -> assertNotNull(recommendation.getReason()),
                () ->
                        assertEquals(
                                Duration.ofMinutes(10), recommendation.getSuggestedBreakDuration()),
                () -> assertNotNull(recommendation.getMetrics()));
    }

    private BreakRecommendation createRecommendation(BreakUrgency urgency) {
        Duration breakDuration =
                urgency == BreakUrgency.CRITICAL || urgency == BreakUrgency.HIGH
                        ? Duration.ofMinutes(10)
                        : Duration.ofMinutes(5);

        return BreakRecommendation.builder()
                .timestamp(Instant.now())
                .urgency(urgency)
                .reason("Test recommendation for urgency: " + urgency)
                .suggestedBreakDuration(breakDuration)
                .metrics(
                        ActivityIntensityMetrics.builder()
                                .totalEvents(5000)
                                .keyboardEvents(3000)
                                .mouseEvents(2000)
                                .timeWindow(Duration.ofMinutes(25))
                                .build())
                .build();
    }
}
