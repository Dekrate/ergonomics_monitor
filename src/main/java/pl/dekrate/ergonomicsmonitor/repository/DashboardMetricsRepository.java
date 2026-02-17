package pl.dekrate.ergonomicsmonitor.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pl.dekrate.ergonomicsmonitor.model.DashboardMetricsEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * R2DBC repository for {@link DashboardMetricsEntity} providing reactive database operations.
 *
 * Demonstrates best practices:
 * - Use declarative methods for simple queries (performance is identical)
 * - Use @Query for aggregations, complex joins, and PostgreSQL-specific features
 * - Use @Query for performance-critical operations needing specific SQL
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface DashboardMetricsRepository extends R2dbcRepository<DashboardMetricsEntity, UUID> {

    /**
     * Finds dashboard metrics for a specific user and date.
     *
     * @param userId the user identifier
     * @param metricDate the date for metrics
     * @return mono with dashboard metrics, empty if not found
     */
    Mono<DashboardMetricsEntity> findByUserIdAndMetricDate(UUID userId, LocalDate metricDate);

    /**
     * Finds dashboard metrics for a user within a date range, ordered by date descending.
     * Used for weekly/monthly dashboard views.
     *
     * @param userId the user identifier
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return flux of dashboard metrics within the date range
     */
    Flux<DashboardMetricsEntity> findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(
            UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Finds the latest dashboard metrics for a user.
     * Useful for showing the most recent activity summary.
     *
     * @param userId the user identifier
     * @return mono with the latest dashboard metrics, empty if none found
     */
    @Query("""
        SELECT * FROM dashboard_metrics 
        WHERE user_id = :userId 
        ORDER BY metric_date DESC 
        LIMIT 1
    """)
    Mono<DashboardMetricsEntity> findLatestByUserId(UUID userId);

    /**
     * Finds dashboard metrics for the last N days for a user.
     *
     * @param userId the user identifier
     * @param days number of days to look back
     * @return flux of recent dashboard metrics
     */
    @Query("""
        SELECT * FROM dashboard_metrics 
        WHERE user_id = :userId 
        AND metric_date >= CURRENT_DATE - INTERVAL ':days days'
        ORDER BY metric_date DESC
    """)
    Flux<DashboardMetricsEntity> findRecentByUserId(UUID userId, int days);

    /**
     * Calculates average productivity score for a user over a date range.
     *
     * @param userId the user identifier
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return mono with average productivity score, null if no data
     */
    @Query("""
        SELECT AVG(productivity_score) FROM dashboard_metrics 
        WHERE user_id = :userId 
        AND metric_date BETWEEN :startDate AND :endDate
        AND productivity_score IS NOT NULL
    """)
    Mono<Double> calculateAverageProductivityScore(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Finds dates that need metric updates (no metrics exist for those dates).
     * Used by scheduled jobs to identify missing aggregations.
     *
     * @param userId the user identifier
     * @param startDate start date to check
     * @param endDate end date to check
     * @return flux of dates missing metrics
     */
    @Query("""
        SELECT DISTINCT DATE(ae.timestamp) as missing_date
        FROM activity_events ae
        WHERE ae.user_id = :userId
        AND DATE(ae.timestamp) BETWEEN :startDate AND :endDate
        AND NOT EXISTS (
            SELECT 1 FROM dashboard_metrics dm 
            WHERE dm.user_id = ae.user_id 
            AND dm.metric_date = DATE(ae.timestamp)
        )
        ORDER BY missing_date
    """)
    Flux<LocalDate> findMissingMetricDates(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Updates or inserts dashboard metrics using upsert operation.
     * Uses PostgreSQL's ON CONFLICT clause for efficient upsert.
     *
     * @param entity the dashboard metrics entity to upsert
     * @return mono with the saved entity
     */
    @Query("""
        INSERT INTO dashboard_metrics (
            id, user_id, metric_date, total_events, avg_intensity, max_intensity,
            break_recommendations_count, work_duration_minutes, break_duration_minutes,
            productivity_score, metadata, created_at, updated_at
        ) VALUES (
            :#{#entity.id}, :#{#entity.userId}, :#{#entity.metricDate}, :#{#entity.totalEvents},
            :#{#entity.avgIntensity}, :#{#entity.maxIntensity}, :#{#entity.breakRecommendationsCount},
            :#{#entity.workDurationMinutes}, :#{#entity.breakDurationMinutes}, :#{#entity.productivityScore},
            :#{#entity.metadata}, :#{#entity.createdAt}, :#{#entity.updatedAt}
        )
        ON CONFLICT (user_id, metric_date) 
        DO UPDATE SET
            total_events = EXCLUDED.total_events,
            avg_intensity = EXCLUDED.avg_intensity,
            max_intensity = EXCLUDED.max_intensity,
            break_recommendations_count = EXCLUDED.break_recommendations_count,
            work_duration_minutes = EXCLUDED.work_duration_minutes,
            break_duration_minutes = EXCLUDED.break_duration_minutes,
            productivity_score = EXCLUDED.productivity_score,
            metadata = EXCLUDED.metadata,
            updated_at = EXCLUDED.updated_at
        RETURNING *
    """)
    Mono<DashboardMetricsEntity> upsert(DashboardMetricsEntity entity);

    /**
     * Deletes dashboard metrics older than specified number of days.
     * Used for cleanup and data retention management.
     *
     * @param retentionDays number of days to retain
     * @return mono with number of deleted rows
     */
    @Query("""
        DELETE FROM dashboard_metrics 
        WHERE metric_date < CURRENT_DATE - INTERVAL ':retentionDays days'
    """)
    Mono<Integer> deleteOlderThan(int retentionDays);
}
