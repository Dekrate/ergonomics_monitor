package pl.dekrate.ergonomicsmonitor.service.strategy;

import java.util.List;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for analyzing activity patterns and determining break recommendations.
 * Implements Strategy Pattern to allow different algorithms for break detection.
 *
 * <p>Following SOLID principles: - Single Responsibility: Each strategy focuses on one analysis
 * algorithm - Open/Closed: New strategies can be added without modifying existing code - Dependency
 * Inversion: Clients depend on this abstraction, not concrete implementations
 *
 * @author dekrate
 * @version 1.0
 */
public interface IntensityAnalysisStrategy {

    /**
     * Analyzes a list of activity events and produces a break recommendation if warranted.
     *
     * @param events list of recent activity events to analyze
     * @return Mono containing a BreakRecommendation if a break is needed, empty Mono otherwise
     */
    Mono<BreakRecommendation> analyze(List<ActivityEvent> events);

    /**
     * Returns a descriptive name for this strategy.
     *
     * @return strategy name
     */
    String getStrategyName();
}
