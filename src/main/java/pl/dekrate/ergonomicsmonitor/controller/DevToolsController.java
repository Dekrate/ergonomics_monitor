package pl.dekrate.ergonomicsmonitor.controller;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.ActivityIntensityMetrics;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import pl.dekrate.ergonomicsmonitor.service.BreakNotificationService;
import pl.dekrate.ergonomicsmonitor.service.notification.BreakNotifier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Controller for developer tools and testing utilities. Available only in 'dev' profile. */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevToolsController {

    private static final Logger log = LoggerFactory.getLogger(DevToolsController.class);
    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final BreakNotificationService breakService;
    private final ActivityRepository repository;
    private final java.util.List<BreakNotifier> notifiers;

    public DevToolsController(
            BreakNotificationService breakService,
            ActivityRepository repository,
            java.util.List<BreakNotifier> notifiers) {
        this.breakService = breakService;
        this.repository = repository;
        this.notifiers = notifiers;
    }

    /**
     * Forces immediate break check, bypassing the scheduler. Use this to trigger AI analysis on
     * demand.
     */
    @PostMapping("/trigger-check")
    public Mono<String> triggerCheck() {
        log.info("Manual trigger of break analysis...");
        breakService.checkAndNotifyIfBreakNeeded();
        return Mono.just("Analysis triggered. Check application logs or system notifications.");
    }

    /** Sends a direct test notification through all configured notifiers (bypasses analysis). */
    @PostMapping("/test-notification")
    public Mono<String> testNotification() {
        BreakRecommendation recommendation =
                BreakRecommendation.builder()
                        .timestamp(Instant.now())
                        .urgency(BreakUrgency.CRITICAL)
                        .reason("Manual dev test notification")
                        .suggestedBreakDuration(java.time.Duration.ofMinutes(5))
                        .metrics(
                                ActivityIntensityMetrics.builder()
                                        .totalEvents(5000)
                                        .keyboardEvents(3000)
                                        .mouseEvents(2000)
                                        .timeWindow(java.time.Duration.ofMinutes(25))
                                        .build())
                        .build();

        return Flux.fromIterable(notifiers)
                .flatMap(
                        notifier ->
                                notifier.sendNotification(recommendation)
                                        .thenReturn("OK: " + notifier.getNotifierType())
                                        .onErrorResume(
                                                err ->
                                                        Mono.just(
                                                                "ERR: "
                                                                        + notifier.getNotifierType()
                                                                        + " -> "
                                                                        + err.getClass()
                                                                                .getSimpleName())))
                .collectList()
                .map(results -> results.stream().collect(Collectors.joining("; ")));
    }

    /**
     * Seeds artificial high-intensity activity to simulate work. Useful for testing AI thresholds.
     *
     * @param intensity Intensity level (default 5000)
     */
    @PostMapping("/seed-activity")
    public Mono<String> seedActivity(@RequestParam(defaultValue = "5000") double intensity) {
        log.info("Seeding artificial activity with intensity: {}", intensity);

        ActivityEvent event =
                ActivityEvent.builder()
                        .id(UUID.randomUUID())
                        .userId(SYSTEM_USER_ID)
                        .timestamp(Instant.now())
                        .type(ActivityType.SYSTEM_EVENT)
                        .intensity(intensity)
                        .metadata(
                                Map.of(
                                        "source",
                                        "dev-tools",
                                        "simulated",
                                        true,
                                        "total_count",
                                        (long) intensity,
                                        "keyboard_count",
                                        (long) (intensity * 0.6),
                                        "mouse_count",
                                        (long) (intensity * 0.4)))
                        .build();

        return repository
                .save(event)
                .map(
                        saved ->
                                "Saved simulated event: "
                                        + saved.getId()
                                        + " with intensity "
                                        + saved.getIntensity());
    }
}
