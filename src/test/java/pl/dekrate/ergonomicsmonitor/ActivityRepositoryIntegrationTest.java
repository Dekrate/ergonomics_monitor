package pl.dekrate.ergonomicsmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;
import pl.dekrate.ergonomicsmonitor.repository.ActivityEventRepository;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for the database layer.
 * Uses Testcontainers to spin up a real PostgreSQL instance.
 * Verifies that custom R2DBC converters correctly handle JSONB metadata mapping.
 */
@SpringBootTest
@Testcontainers
class ActivityRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ActivityEventRepository repository;

    @Test
    void shouldSaveAndRetrieveEventWithMetadata() {
        // Given
        UUID userId = UUID.randomUUID();
        Map<String, Object> metadata = Map.of("key", "value", "count", 42);
        
        ActivityEvent event = ActivityEvent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .timestamp(Instant.now())
                .type(ActivityType.SYSTEM_EVENT)
                .intensity(10.5)
                .metadata(metadata)
                .build();

        // When & Then
        repository.save(event)
                .flatMap(saved -> repository.findById(saved.getId()))
                .as(StepVerifier::create)
                .assertNext(retrieved -> {
                    assertEquals(userId, retrieved.getUserId());
                    assertEquals("value", retrieved.getMetadata().get("key"));
                    assertEquals(42, retrieved.getMetadata().get("count"));
                })
                .verifyComplete();
    }
}
