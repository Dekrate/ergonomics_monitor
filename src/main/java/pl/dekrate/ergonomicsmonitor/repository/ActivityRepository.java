package pl.dekrate.ergonomicsmonitor.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import reactor.core.publisher.Flux;

@Repository
public interface ActivityRepository extends R2dbcRepository<ActivityEvent, UUID> {

    @Query(
            """
        SELECT * FROM activity_events
        ORDER BY timestamp DESC
        LIMIT 50
    """)
    Flux<ActivityEvent> findLatest50Events();

    @Query(
            """
        SELECT * FROM activity_events
        WHERE user_id = :userId
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp ASC
    """)
    Flux<ActivityEvent> findByUserIdAndTimestampBetween(
            UUID userId, Instant startTime, Instant endTime);
}
