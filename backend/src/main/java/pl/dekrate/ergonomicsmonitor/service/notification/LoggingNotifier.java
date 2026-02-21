package pl.dekrate.ergonomicsmonitor.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import reactor.core.publisher.Mono;

/**
 * Logging-only notifier for testing and development environments. Useful when running on
 * non-Windows platforms or when MessageBox is disruptive.
 *
 * @author dekrate
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "ergonomics.notifications.log-only.enabled", havingValue = "true")
public class LoggingNotifier implements BreakNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

    @Override
    public Mono<Void> sendNotification(BreakRecommendation recommendation) {
        return Mono.fromRunnable(
                        () -> {
                            log.warn("=== BREAK NOTIFICATION ===");
                            log.warn("Urgency: {}", recommendation.getUrgency());
                            log.warn("Reason: {}", recommendation.getReason());
                            log.warn(
                                    "Suggested break: {} minutes",
                                    recommendation.getSuggestedBreakDuration().toMinutes());
                            log.warn("Metrics: {}", recommendation.getMetrics());
                            log.warn("=========================");
                        })
                .then();
    }

    @Override
    public String getNotifierType() {
        return "Logging Only";
    }
}
