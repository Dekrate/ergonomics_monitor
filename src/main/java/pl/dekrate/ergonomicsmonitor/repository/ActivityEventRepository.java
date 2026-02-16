package pl.dekrate.ergonomicsmonitor.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import java.util.UUID;

@Repository
public interface ActivityEventRepository extends ReactiveCrudRepository<ActivityEvent, UUID> {
}
