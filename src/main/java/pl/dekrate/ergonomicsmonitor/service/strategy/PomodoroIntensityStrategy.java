package pl.dekrate.ergonomicsmonitor.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.ActivityIntensityMetrics;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Concrete strategy implementing Pomodoro-inspired break detection.
 * Recommends breaks after sustained periods of intensive work.
 * <p>
 * Algorithm:
 * - Analyzes events from last 25 minutes (standard Pomodoro duration)
 * - If average intensity >100 events/min → recommend 5-minute break
 * - If average intensity >200 events/min → recommend 10-minute break (critical)
 *
 * @author dekrate
 * @version 1.0
 */
@Component
public class PomodoroIntensityStrategy implements IntensityAnalysisStrategy {

    private static final Logger log = LoggerFactory.getLogger(PomodoroIntensityStrategy.class);

    private static final Duration POMODORO_DURATION = Duration.ofMinutes(25);
    private static final Duration SHORT_BREAK = Duration.ofMinutes(5);
    private static final Duration LONG_BREAK = Duration.ofMinutes(10);


    @Override
    public Mono<BreakRecommendation> analyze(List<ActivityEvent> events) {
        if (events == null || events.isEmpty()) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
            ActivityIntensityMetrics metrics = calculateMetrics(events);
            log.debug("Pomodoro analysis: {}", metrics);

            if (metrics.isCritical()) {
                return Optional.of(createCriticalBreakRecommendation(metrics));
            } else if (metrics.isIntensive()) {
                return Optional.of(createModerateBreakRecommendation(metrics));
            }

            return Optional.<BreakRecommendation>empty();
        }).flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    private ActivityIntensityMetrics calculateMetrics(List<ActivityEvent> events) {
        long totalEvents = 0;
        long keyboardCount = 0;
        long mouseCount = 0;

        for (ActivityEvent event : events) {
            Map<String, Object> metadata = event.getMetadata();
            if (metadata != null) {
                totalEvents += getLongFromMetadata(metadata, "total_count");
                keyboardCount += getLongFromMetadata(metadata, "keyboard_count");
                mouseCount += getLongFromMetadata(metadata, "mouse_count");
            }
        }

        return ActivityIntensityMetrics.builder()
                .totalEvents(totalEvents)
                .keyboardEvents(keyboardCount)
                .mouseEvents(mouseCount)
                .timeWindow(POMODORO_DURATION)
                .build();
    }

    private long getLongFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private BreakRecommendation createCriticalBreakRecommendation(ActivityIntensityMetrics metrics) {
        return BreakRecommendation.builder()
                .timestamp(Instant.now())
                .urgency(BreakUrgency.CRITICAL)
                .reason(String.format(
                    "Wykryto bardzo intensywną pracę: %.0f zdarzeń/minutę przez 25 minut. " +
                    "Natychmiastowa przerwa zalecana aby zapobiec RSI (Repetitive Strain Injury).",
                    metrics.getEventsPerMinute()
                ))
                .suggestedBreakDuration(LONG_BREAK)
                .metrics(metrics)
                .build();
    }

    private BreakRecommendation createModerateBreakRecommendation(ActivityIntensityMetrics metrics) {
        return BreakRecommendation.builder()
                .timestamp(Instant.now())
                .urgency(BreakUrgency.MEDIUM)
                .reason(String.format(
                    "Intensywna praca wykryta: %.0f zdarzeń/minutę. " +
                    "Zalecana 5-minutowa przerwa zgodnie z techniką Pomodoro.",
                    metrics.getEventsPerMinute()
                ))
                .suggestedBreakDuration(SHORT_BREAK)
                .metrics(metrics)
                .build();
    }

    @Override
    public String getStrategyName() {
        return "Pomodoro Intensity Strategy";
    }
}
