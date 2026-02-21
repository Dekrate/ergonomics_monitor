package pl.dekrate.ergonomicsmonitor.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.*;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import pl.dekrate.ergonomicsmonitor.service.notification.BreakNotifier;
import pl.dekrate.ergonomicsmonitor.service.strategy.IntensityAnalysisStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Comprehensive unit tests for BreakNotificationService. Uses Mockito for mocking dependencies to
 * achieve true unit test isolation.
 *
 * @author dekrate
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BreakNotificationService")
class BreakNotificationServiceTest {

    @Mock private ActivityRepository repository;

    @Mock private IntensityAnalysisStrategy strategy;

    @Mock private BreakNotifier notifier;

    private BreakNotificationService service;

    @BeforeEach
    void setUp() {
        service = new BreakNotificationService(repository, List.of(strategy), List.of(notifier));
        service.resetNotificationThrottle();
    }

    @Test
    @DisplayName("should fetch events and trigger notification when recommendation is made")
    void shouldFetchAndNotify() {
        // given
        List<ActivityEvent> mockEvents = List.of(createMockEvent());
        BreakRecommendation mockRecommendation = createMockRecommendation();

        when(repository.findLatest50Events()).thenReturn(Flux.fromIterable(mockEvents));
        when(strategy.analyze(anyList())).thenReturn(Mono.just(mockRecommendation));
        when(notifier.sendNotification(any())).thenReturn(Mono.empty());

        // when
        service.checkAndNotifyIfBreakNeeded();

        // then
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            verify(repository, times(1)).findLatest50Events();
                            verify(strategy, times(1)).analyze(anyList());
                            verify(notifier, times(1)).sendNotification(mockRecommendation);
                        });
    }

    @Test
    @DisplayName("should not notify when no recommendation is made")
    void shouldNotNotifyWhenNoRecommendation() {
        // given
        List<ActivityEvent> mockEvents = List.of(createMockEvent());

        when(repository.findLatest50Events()).thenReturn(Flux.fromIterable(mockEvents));
        when(strategy.analyze(anyList())).thenReturn(Mono.empty());

        // when
        service.checkAndNotifyIfBreakNeeded();

        // then
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            verify(repository, times(1)).findLatest50Events();
                            verify(strategy, times(1)).analyze(anyList());
                            verify(notifier, never()).sendNotification(any());
                        });
    }

    @Test
    @DisplayName("should not notify when no events are available")
    void shouldNotNotifyWhenNoEvents() {
        // given
        when(repository.findLatest50Events()).thenReturn(Flux.empty());

        // when
        service.checkAndNotifyIfBreakNeeded();

        // then
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            verify(repository, times(1)).findLatest50Events();
                            verify(strategy, never()).analyze(anyList());
                            verify(notifier, never()).sendNotification(any());
                        });
    }

    @Test
    @DisplayName("should throttle notifications - not send within 10 minutes")
    void shouldThrottleNotifications() {
        // given
        List<ActivityEvent> mockEvents = List.of(createMockEvent());
        BreakRecommendation mockRecommendation = createMockRecommendation();

        when(repository.findLatest50Events()).thenReturn(Flux.fromIterable(mockEvents));
        when(strategy.analyze(anyList())).thenReturn(Mono.just(mockRecommendation));
        when(notifier.sendNotification(any())).thenReturn(Mono.empty());

        // when - first call should notify
        service.checkAndNotifyIfBreakNeeded();
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(notifier, times(1)).sendNotification(any()));

        // when - second call immediately after should be throttled
        service.checkAndNotifyIfBreakNeeded();

        // then - notifier should only be called once
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(notifier, times(1)).sendNotification(any()));
    }

    @Test
    @DisplayName("should handle notifier failure gracefully and continue")
    void shouldHandleNotifierFailureGracefully() {
        // given
        List<ActivityEvent> mockEvents = List.of(createMockEvent());
        BreakRecommendation mockRecommendation = createMockRecommendation();

        when(repository.findLatest50Events()).thenReturn(Flux.fromIterable(mockEvents));
        when(strategy.analyze(anyList())).thenReturn(Mono.just(mockRecommendation));
        when(notifier.sendNotification(any()))
                .thenReturn(Mono.error(new RuntimeException("Notification failed")));

        // when - should not throw exception
        assertDoesNotThrow(() -> service.checkAndNotifyIfBreakNeeded());

        // then
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(notifier, times(1)).sendNotification(any()));
    }

    @Test
    @DisplayName("should use first strategy that produces a recommendation")
    void shouldUseFirstStrategyWithRecommendation() {
        // given
        IntensityAnalysisStrategy strategy1 = mock(IntensityAnalysisStrategy.class);
        IntensityAnalysisStrategy strategy2 = mock(IntensityAnalysisStrategy.class);

        service =
                new BreakNotificationService(
                        repository, List.of(strategy1, strategy2), List.of(notifier));
        service.resetNotificationThrottle();

        List<ActivityEvent> mockEvents = List.of(createMockEvent());
        BreakRecommendation mockRecommendation = createMockRecommendation();

        when(repository.findLatest50Events()).thenReturn(Flux.fromIterable(mockEvents));
        when(strategy1.analyze(anyList())).thenReturn(Mono.just(mockRecommendation));
        when(notifier.sendNotification(any())).thenReturn(Mono.empty());

        // when
        service.checkAndNotifyIfBreakNeeded();

        // then - only first strategy should be called
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            verify(strategy1, times(1)).analyze(anyList());
                            verify(strategy2, never()).analyze(anyList());
                        });
    }

    @Test
    @DisplayName("should track last notification time correctly")
    void shouldTrackLastNotificationTime() {
        // given
        List<ActivityEvent> mockEvents = List.of(createMockEvent());
        BreakRecommendation mockRecommendation = createMockRecommendation();

        when(repository.findLatest50Events()).thenReturn(Flux.fromIterable(mockEvents));
        when(strategy.analyze(anyList())).thenReturn(Mono.just(mockRecommendation));
        when(notifier.sendNotification(any())).thenReturn(Mono.empty());

        // when
        assertTrue(
                service.getLastNotificationTime().isEmpty(),
                "Should start with no notification time");

        service.checkAndNotifyIfBreakNeeded();

        // then
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(
                        () ->
                                assertTrue(
                                        service.getLastNotificationTime().isPresent(),
                                        "Should have notification time after sending"));
    }

    // Helper methods
    private ActivityEvent createMockEvent() {
        return ActivityEvent.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .timestamp(Instant.now())
                .type(ActivityType.SYSTEM_EVENT)
                .intensity(100.0)
                .metadata(
                        Map.of(
                                "total_count", 100L,
                                "keyboard_count", 60L,
                                "mouse_count", 40L))
                .build();
    }

    private BreakRecommendation createMockRecommendation() {
        return BreakRecommendation.builder()
                .timestamp(Instant.now())
                .urgency(BreakUrgency.MEDIUM)
                .reason("Test recommendation")
                .suggestedBreakDuration(Duration.ofMinutes(5))
                .metrics(
                        ActivityIntensityMetrics.builder()
                                .totalEvents(100)
                                .keyboardEvents(60)
                                .mouseEvents(40)
                                .timeWindow(Duration.ofMinutes(10))
                                .build())
                .build();
    }
}
