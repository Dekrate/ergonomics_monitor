package pl.dekrate.ergonomicsmonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import pl.dekrate.ergonomicsmonitor.service.notification.BreakNotifier;
import pl.dekrate.ergonomicsmonitor.service.strategy.IntensityAnalysisStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core service orchestrating break notification logic.
 * <p>
 * Responsibilities (following SRP):
 * - Schedule periodic activity analysis
 * - Coordinate between strategy and notification layers
 * - Maintain state to prevent notification spam
 * <p>
 * Design principles applied:
 * - Dependency Inversion: Depends on abstractions (IntensityAnalysisStrategy, BreakNotifier)
 * - Single Responsibility: Only orchestrates, delegates actual work
 * - Open/Closed: New strategies/notifiers can be added without modifying this class
 *
 * @author dekrate
 * @version 1.0
 */
@Service
public class BreakNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BreakNotificationService.class);

    private static final Duration MIN_TIME_BETWEEN_NOTIFICATIONS = Duration.ofMinutes(10);

    private final ActivityRepository repository;
    private final List<IntensityAnalysisStrategy> analysisStrategies;
    private final List<BreakNotifier> notifiers;

    private volatile Instant lastNotificationTime = Instant.EPOCH;

    /**
     * Constructor injection ensures all dependencies are provided and immutable.
     * Spring automatically injects all beans implementing the interface types.
     */
    public BreakNotificationService(
            ActivityRepository repository,
            List<IntensityAnalysisStrategy> analysisStrategies,
            List<BreakNotifier> notifiers
    ) {
        this.repository = repository;
        this.analysisStrategies = analysisStrategies;
        this.notifiers = notifiers;

        log.info("BreakNotificationService initialized with {} strategies and {} notifiers",
                analysisStrategies.size(), notifiers.size());

        analysisStrategies.forEach(s -> log.info("  - Strategy: {}", s.getStrategyName()));
        notifiers.forEach(n -> log.info("  - Notifier: {}", n.getNotifierType()));
    }

    /**
     * Scheduled task running every minute to check if user needs a break.
     * Uses cron expression for precise scheduling.
     */
    @Scheduled(cron = "${ergonomics.break-check.cron:0 * * * * *}") // Every minute by default
    public void checkAndNotifyIfBreakNeeded() {
        if (shouldSkipNotification()) {
            log.trace("Skipping notification check - too soon since last notification");
            return;
        }

        log.debug("Running break analysis check...");

        fetchRecentEvents()
                .flatMap(this::analyzeWithAllStrategies)
                .flatMap(this::sendNotifications)
                .doOnNext(recommendation -> handleSuccessfulNotification())
                .doOnError(err -> log.error("Error during break analysis", err))
                .subscribe();
    }

    private void handleSuccessfulNotification() {
        updateLastNotificationTime();
    }

    /**
     * Fetches recent activity events from the repository.
     * Limits to last 50 events to analyze approximately 25 minutes of activity.
     */
    private Mono<List<ActivityEvent>> fetchRecentEvents() {
        return repository.findAll()
                .take(50)
                .collectList()
                .doOnNext(events -> log.debug("Fetched {} events for analysis", events.size()));
    }

    /**
     * Runs all registered analysis strategies and returns the first recommendation found.
     * Uses Flux to process strategies in order until one produces a result.
     */
    private Mono<BreakRecommendation> analyzeWithAllStrategies(List<ActivityEvent> events) {
        if (events.isEmpty()) {
            log.debug("No events to analyze");
            return Mono.empty();
        }

        return Flux.fromIterable(analysisStrategies)
                .concatMap(strategy -> {
                    log.trace("Applying strategy: {}", strategy.getStrategyName());
                    return strategy.analyze(events);
                })
                .next() // Take first recommendation
                .doOnNext(rec -> log.info("Break recommendation generated: urgency={}, reason={}",
                        rec.getUrgency(), rec.getReason()));
    }

    /**
     * Sends break recommendation through all registered notifiers in parallel.
     */
    @SuppressWarnings("java:S1602") // Lambda parameter intentionally unused
    private Mono<BreakRecommendation> sendNotifications(BreakRecommendation recommendation) {
        return Flux.fromIterable(notifiers)
                .flatMap(notifier -> {
                    log.debug("Sending notification via: {}", notifier.getNotifierType());
                    return notifier.sendNotification(recommendation)
                            .doOnSuccess(ignored -> log.info("Notification sent successfully via {}",
                                    notifier.getNotifierType()))
                            .onErrorResume(err -> {
                                log.error("Failed to send notification via {}",
                                        notifier.getNotifierType(), err);
                                return Mono.empty(); // Continue with other notifiers
                            });
                })
                .then(Mono.just(recommendation));
    }

    private boolean shouldSkipNotification() {
        Duration timeSinceLastNotification = Duration.between(lastNotificationTime, Instant.now());
        return timeSinceLastNotification.compareTo(MIN_TIME_BETWEEN_NOTIFICATIONS) < 0;
    }

    private void updateLastNotificationTime() {
        lastNotificationTime = Instant.now();
        log.debug("Updated last notification time to: {}", lastNotificationTime);
    }

    /**
     * For testing: allows resetting notification throttle.
     * Package-private for test access.
     */
    @SuppressWarnings("java:S1144") // Used by tests
    void resetNotificationThrottle() {
        lastNotificationTime = Instant.EPOCH;
    }

    /**
     * For testing: provides last notification time.
     * Package-private for test access.
     */
    @SuppressWarnings("java:S1144") // Used by tests
    Optional<Instant> getLastNotificationTime() {
        return lastNotificationTime.equals(Instant.EPOCH)
                ? Optional.empty()
                : Optional.of(lastNotificationTime);
    }
}
