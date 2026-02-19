package pl.dekrate.ergonomicsmonitor.service.notification;

import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import reactor.core.publisher.Mono;

/**
 * Port interface for sending break notifications to users.
 * Implements Hexagonal Architecture (Ports & Adapters pattern).
 * <p>
 * This abstraction allows:
 * - Multiple notification implementations (Windows, Email, SMS, etc.)
 * - Easy testing with mock implementations
 * - Runtime switching between notification methods
 *
 * @author dekrate
 * @version 1.0
 */
public interface BreakNotifier {

    /**
     * Sends a break notification to the user based on the recommendation.
     *
     * @param recommendation the break recommendation to notify about
     * @return Mono completing when notification is sent, or error if sending fails
     */
    Mono<Void> sendNotification(BreakRecommendation recommendation);

    /**
     * Returns the type/name of this notifier for logging purposes.
     *
     * @return notifier type description
     */
    String getNotifierType();
}

