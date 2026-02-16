package pl.dekrate.ergonomicsmonitor;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a captured or aggregated activity event.
 * Maps to the <code>activity_events</code> table in the PostgreSQL database.
 * <p>
 * Supports both raw hardware events and aggregated summary events.
 */
@Table("activity_events")
public class ActivityEvent {

    @Id
    private UUID id;
    private UUID userId;
    private Instant timestamp;
    private ActivityType type;
    private double intensity;
    private Map<String, Object> metadata;

    /**
     * Default constructor for R2DBC deserialization.
     */
    public ActivityEvent() {}

    /**
     * Full constructor for creating instances manually.
     */
    public ActivityEvent(UUID id, UUID userId, Instant timestamp, ActivityType type, double intensity, Map<String, Object> metadata) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
        this.type = type;
        this.intensity = intensity;
        this.metadata = metadata;
    }

    /**
     * Returns a new builder instance for fluent object creation.
     * @return a new ActivityEventBuilder
     */
    public static ActivityEventBuilder builder() {
        return new ActivityEventBuilder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
    public double getIntensity() { return intensity; }
    public void setIntensity(double intensity) { this.intensity = intensity; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    /**
     * Builder pattern implementation for ActivityEvent.
     */
    public static class ActivityEventBuilder {
        private UUID id;
        private UUID userId;
        private Instant timestamp;
        private ActivityType type;
        private double intensity;
        private Map<String, Object> metadata;

        public ActivityEventBuilder id(UUID id) { this.id = id; return this; }
        public ActivityEventBuilder userId(UUID userId) { this.userId = userId; return this; }
        public ActivityEventBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public ActivityEventBuilder type(ActivityType type) { this.type = type; return this; }
        public ActivityEventBuilder intensity(double intensity) { this.intensity = intensity; return this; }
        public ActivityEventBuilder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public ActivityEvent build() {
            return new ActivityEvent(id, userId, timestamp, type, intensity, metadata);
        }
    }
}
