package pl.dekrate.ergonomicsmonitor.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendationEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC repository for {@link BreakRecommendationEntity} providing reactive database operations.
 *
 * Supports querying break recommendations by various criteria including user, time ranges,
 * and AI-generated status for analytics and dashboard purposes.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface BreakRecommendationRepository extends R2dbcRepository<BreakRecommendationEntity, UUID> {

    /**
     * Finds all break recommendations for a specific user ordered by creation time (newest first).
     *
     * @param userId the user identifier
     * @return flux of break recommendations
     */
    Flux<BreakRecommendationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds break recommendations for a user within a specific time range.
     * Useful for dashboard analytics and reporting.
     *
     * @param userId the user identifier
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return flux of break recommendations within the time range
     */
    @Query("""
        SELECT * FROM break_recommendations 
        WHERE user_id = :userId 
        AND created_at BETWEEN :startTime AND :endTime 
        ORDER BY created_at DESC
    """)
    Flux<BreakRecommendationEntity> findByUserIdAndTimeRange(UUID userId, Instant startTime, Instant endTime);

    /**
     * Finds AI-generated break recommendations for analytics on AI performance.
     *
     * @param userId the user identifier
     * @param aiGenerated whether to find AI-generated (true) or manual (false) recommendations
     * @return flux of break recommendations filtered by AI generation status
     */
    Flux<BreakRecommendationEntity> findByUserIdAndAiGeneratedOrderByCreatedAtDesc(UUID userId, Boolean aiGenerated);

    /**
     * Counts total break recommendations for a user within a date range.
     * Used for dashboard metrics aggregation.
     *
     * @param userId the user identifier
     * @param startTime start of time range (inclusive)
     * @param endTime end of time range (inclusive)
     * @return mono with count of recommendations
     */
    @Query("""
        SELECT COUNT(*) FROM break_recommendations 
        WHERE user_id = :userId 
        AND created_at BETWEEN :startTime AND :endTime
    """)
    Mono<Long> countByUserIdAndTimeRange(UUID userId, Instant startTime, Instant endTime);

    /**
     * Finds the latest break recommendation for a user.
     * Used for determining if enough time has passed since the last recommendation.
     *
     * @param userId the user identifier
     * @return mono with the latest break recommendation, empty if none found
     */
    @Query("""
        SELECT * FROM break_recommendations 
        WHERE user_id = :userId 
        ORDER BY created_at DESC 
        LIMIT 1
    """)
    Mono<BreakRecommendationEntity> findLatestByUserId(UUID userId);

    /**
     * Finds break recommendations by urgency level for analytics.
     *
     * @param userId the user identifier
     * @param urgency the urgency level (LOW, MEDIUM, CRITICAL)
     * @return flux of break recommendations with specified urgency
     */
    Flux<BreakRecommendationEntity> findByUserIdAndUrgencyOrderByCreatedAtDesc(UUID userId, String urgency);

    /**
     * Marks a break recommendation as accepted by the user.
     *
     * @param id the recommendation identifier
     * @param acceptedAt the timestamp when accepted
     * @return mono with number of updated rows
     */
    @Query("""
        UPDATE break_recommendations 
        SET accepted_at = :acceptedAt 
        WHERE id = :id AND accepted_at IS NULL AND dismissed_at IS NULL
    """)
    Mono<Integer> markAsAccepted(UUID id, Instant acceptedAt);

    /**
     * Marks a break recommendation as dismissed by the user.
     *
     * @param id the recommendation identifier
     * @param dismissedAt the timestamp when dismissed
     * @return mono with number of updated rows
     */
    @Query("""
        UPDATE break_recommendations 
        SET dismissed_at = :dismissedAt 
        WHERE id = :id AND accepted_at IS NULL AND dismissed_at IS NULL
    """)
    Mono<Integer> markAsDismissed(UUID id, Instant dismissedAt);
}
