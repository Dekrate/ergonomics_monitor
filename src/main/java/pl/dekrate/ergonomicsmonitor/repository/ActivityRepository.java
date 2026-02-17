package pl.dekrate.ergonomicsmonitor.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.ActivitySummary;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ActivityRepository extends R2dbcRepository<ActivityEvent, UUID> {

    @Query("""
        SELECT * FROM activity_events 
        WHERE user_id = :userId 
        AND timestamp > :since 
        ORDER BY timestamp DESC
    """)
    Flux<ActivityEvent> findRecentActivity(UUID userId, Instant since);

    @Query("""
        SELECT * FROM activity_events 
        WHERE user_id = :userId 
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp ASC
    """)
    Flux<ActivityEvent> findByUserIdAndTimestampBetween(UUID userId, Instant startTime, Instant endTime);

    @Query("""
        SELECT 
            date_trunc('minute', timestamp) as minute,
            AVG(intensity) as avg_intensity,
            COUNT(*) as event_count
        FROM activity_events
        WHERE user_id = :userId
        AND timestamp > :since
        GROUP BY date_trunc('minute', timestamp)
        ORDER BY minute
    """)
    Flux<ActivitySummary> getIntensityTimeline(UUID userId, Instant since);
}
