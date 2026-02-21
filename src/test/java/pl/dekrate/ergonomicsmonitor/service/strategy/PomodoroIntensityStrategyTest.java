package pl.dekrate.ergonomicsmonitor.service.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Comprehensive tests for PomodoroIntensityStrategy. Tests all business scenarios including edge
 * cases.
 *
 * @author dekrate
 * @version 1.0
 */
@DisplayName("PomodoroIntensityStrategy")
class PomodoroIntensityStrategyTest {

    private PomodoroIntensityStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PomodoroIntensityStrategy();
    }

    @Test
    @DisplayName("should return empty Mono when events list is null")
    void shouldReturnEmptyWhenEventsNull() {
        // when
        Mono<BreakRecommendation> result = strategy.analyze(null);

        // then
        StepVerifier.create(result).expectComplete().verify();
    }

    @Test
    @DisplayName("should return empty Mono when events list is empty")
    void shouldReturnEmptyWhenEventsEmpty() {
        // given
        List<ActivityEvent> events = new ArrayList<>();

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then
        StepVerifier.create(result).expectComplete().verify();
    }

    @Test
    @DisplayName("should return empty Mono when intensity is below threshold")
    void shouldReturnEmptyWhenIntensityLow() {
        // given - 50 events/min (below 100 threshold)
        List<ActivityEvent> events = List.of(createEvent(50, 30, 20));

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then
        StepVerifier.create(result).expectComplete().verify();
    }

    @Test
    @DisplayName(
            "should return MEDIUM urgency recommendation when intensity is moderate (100-200 events/min)")
    void shouldReturnMediumUrgencyForModerateIntensity() {
        // given - simulating 25 minutes of 120 events/min = 3000 total events
        List<ActivityEvent> events =
                List.of(
                        createEvent(1000, 600, 400),
                        createEvent(1000, 600, 400),
                        createEvent(1000, 600, 400));

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then
        StepVerifier.create(result)
                .assertNext(
                        recommendation -> {
                            assertAll(
                                    () ->
                                            assertEquals(
                                                    BreakUrgency.MEDIUM,
                                                    recommendation.getUrgency()),
                                    () ->
                                            assertEquals(
                                                    5,
                                                    recommendation
                                                            .getSuggestedBreakDuration()
                                                            .toMinutes()),
                                    () ->
                                            assertTrue(
                                                    recommendation
                                                            .getReason()
                                                            .contains("Intensywna praca")),
                                    () ->
                                            assertEquals(
                                                    3000,
                                                    recommendation.getMetrics().getTotalEvents()),
                                    () ->
                                            assertEquals(
                                                    1800,
                                                    recommendation
                                                            .getMetrics()
                                                            .getKeyboardEvents()),
                                    () ->
                                            assertEquals(
                                                    1200,
                                                    recommendation.getMetrics().getMouseEvents()));
                        })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName(
            "should return CRITICAL urgency recommendation when intensity is high (>200 events/min)")
    void shouldReturnCriticalUrgencyForHighIntensity() {
        // given - simulating 25 minutes of 250 events/min = 6250 total events
        List<ActivityEvent> events =
                List.of(
                        createEvent(2500, 1500, 1000),
                        createEvent(2500, 1500, 1000),
                        createEvent(1250, 750, 500));

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then
        StepVerifier.create(result)
                .assertNext(
                        recommendation -> {
                            assertAll(
                                    () ->
                                            assertEquals(
                                                    BreakUrgency.CRITICAL,
                                                    recommendation.getUrgency()),
                                    () ->
                                            assertEquals(
                                                    10,
                                                    recommendation
                                                            .getSuggestedBreakDuration()
                                                            .toMinutes()),
                                    () ->
                                            assertTrue(
                                                    recommendation
                                                            .getReason()
                                                            .contains("bardzo intensywną pracę")),
                                    () -> assertTrue(recommendation.getReason().contains("RSI")),
                                    () ->
                                            assertEquals(
                                                    6250,
                                                    recommendation.getMetrics().getTotalEvents()));
                        })
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("should handle boundary case at exactly 100 events/min")
    void shouldHandleBoundaryAtExactly100EventsPerMin() {
        // given - exactly 2500 events over 25 minutes = 100 events/min
        List<ActivityEvent> events = List.of(createEvent(2500, 1500, 1000));

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then - should NOT trigger (threshold is >100, not >=100)
        StepVerifier.create(result).expectComplete().verify();
    }

    @Test
    @DisplayName("should handle boundary case at exactly 200 events/min")
    void shouldHandleBoundaryAtExactly200EventsPerMin() {
        // given - exactly 5000 events over 25 minutes = 200 events/min
        List<ActivityEvent> events = List.of(createEvent(5000, 3000, 2000));

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then - should trigger MEDIUM, not CRITICAL (threshold is >200)
        StepVerifier.create(result)
                .assertNext(
                        recommendation ->
                                assertEquals(BreakUrgency.MEDIUM, recommendation.getUrgency()))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("should handle events with null metadata gracefully")
    void shouldHandleNullMetadataGracefully() {
        // given
        ActivityEvent eventWithNullMetadata =
                ActivityEvent.builder()
                        .id(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .timestamp(Instant.now())
                        .type(ActivityType.SYSTEM_EVENT)
                        .intensity(100.0)
                        .metadata(null)
                        .build();

        List<ActivityEvent> events = List.of(eventWithNullMetadata);

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then - should not crash, should return empty (0 events counted)
        StepVerifier.create(result).expectComplete().verify();
    }

    @Test
    @DisplayName("should handle metadata with non-numeric values")
    void shouldHandleNonNumericMetadata() {
        // given
        ActivityEvent event =
                ActivityEvent.builder()
                        .id(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .timestamp(Instant.now())
                        .type(ActivityType.SYSTEM_EVENT)
                        .intensity(100.0)
                        .metadata(
                                Map.of(
                                        "total_count", "not a number",
                                        "keyboard_count", "also not a number"))
                        .build();

        List<ActivityEvent> events = List.of(event);

        // when
        Mono<BreakRecommendation> result = strategy.analyze(events);

        // then - should handle gracefully and treat as 0
        StepVerifier.create(result).expectComplete().verify();
    }

    @Test
    @DisplayName("should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        // when
        String name = strategy.getStrategyName();

        // then
        assertEquals("Pomodoro Intensity Strategy", name);
    }

    // Helper method to create test events
    private ActivityEvent createEvent(long totalCount, long keyboardCount, long mouseCount) {
        return ActivityEvent.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .timestamp(Instant.now())
                .type(ActivityType.SYSTEM_EVENT)
                .intensity((double) totalCount)
                .metadata(
                        Map.of(
                                "total_count", totalCount,
                                "keyboard_count", keyboardCount,
                                "mouse_count", mouseCount))
                .build();
    }
}
