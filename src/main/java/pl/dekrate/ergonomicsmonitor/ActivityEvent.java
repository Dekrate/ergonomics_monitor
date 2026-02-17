package pl.dekrate.ergonomicsmonitor;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import pl.dekrate.ergonomicsmonitor.model.ActivityType;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a captured or aggregated activity event.
 * Maps to the <code>activity_events</code> table in the PostgreSQL database.
 * <p>
 * Supports both raw hardware events and aggregated summary events.
 */
@Table("activity_events")
public final class ActivityEvent {

    @Id
    private final UUID id;
    private final UUID userId;
    private final Instant timestamp;
    private final ActivityType type;
    private final double intensity;
    private final Map<String, Object> metadata;

    private ActivityEvent(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.type = Objects.requireNonNull(builder.type, "type cannot be null");
        this.intensity = builder.intensity;
        this.metadata = builder.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ActivityType getType() {
        return type;
    }

    public double getIntensity() {
        return intensity;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityEvent that = (ActivityEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ActivityEvent{" +
                "id=" + id +
                ", userId=" + userId +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", intensity=" + intensity +
                ", metadata=" + metadata +
                '}';
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private Instant timestamp;
        private ActivityType type;
        private double intensity;
        private Map<String, Object> metadata;

        private Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder type(ActivityType type) {
            this.type = type;
            return this;
        }

        public Builder intensity(double intensity) {
            this.intensity = intensity;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ActivityEvent build() {
            return new ActivityEvent(this);
        }
    }
}
