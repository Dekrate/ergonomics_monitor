package pl.dekrate.ergonomicsmonitor.repository;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pl.dekrate.ergonomicsmonitor.model.DashboardMetricsEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for {@link DashboardMetricsEntity} providing reactive database operations.
 *
 * <p>Demonstrates best practices: - Use declarative methods for simple queries (performance is
 * identical) - Use @Query for aggregations, complex joins, and PostgreSQL-specific features -
 * Use @Query for performance-critical operations needing specific SQL
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
     * Finds dashboard metrics for a user within a date range, ordered by date descending. Used for
     * weekly/monthly dashboard views.
     *
     * @param userId the user identifier
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return flux of dashboard metrics within the date range
     */
    Flux<DashboardMetricsEntity> findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(
            UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Finds dashboard metrics for the last N days for a user.
     *
     * @param userId the user identifier
     * @param days number of days to look back
     * @return flux of recent dashboard metrics
     */
    @Query(
            """
        SELECT * FROM dashboard_metrics
        WHERE user_id = :userId
        AND metric_date >= CURRENT_DATE - (:days * INTERVAL '1 day')
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
    @Query(
            """
        SELECT AVG(productivity_score) FROM dashboard_metrics
        WHERE user_id = :userId
        AND metric_date BETWEEN :startDate AND :endDate
        AND productivity_score IS NOT NULL
    """)
    Mono<Double> calculateAverageProductivityScore(
            UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Updates or inserts dashboard metrics using upsert operation. Uses PostgreSQL's ON CONFLICT
     * clause for efficient upsert.
     *
     * @param entity the dashboard metrics entity to upsert
     * @return mono with the saved entity
     */
    @Query(
            """
        INSERT INTO dashboard_metrics (
            id, user_id, metric_date, total_events, avg_intensity
        ) VALUES (
            :#{#entity.id}, :#{#entity.userId}, :#{#entity.metricDate}, :#{#entity.totalEvents},
            :#{#entity.avgIntensity}
        )
        ON CONFLICT (user_id, metric_date)
        DO UPDATE SET
            total_events = EXCLUDED.total_events,
            avg_intensity = EXCLUDED.avg_intensity
        RETURNING *
    """)
    Mono<DashboardMetricsEntity> upsert(DashboardMetricsEntity entity);
}
