package pl.dekrate.ergonomicsmonitor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Table("activities")
public final class Activity {

    @Id
    private final Long id;
    private final ActivityType type;
    private final LocalDateTime timestamp;
    private final ActivitySummary summary;

    private Activity(Builder builder) {
        this.id = builder.id;
        this.type = Objects.requireNonNull(builder.type, "type cannot be null");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.summary = Objects.requireNonNull(builder.summary, "summary cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ActivitySummary getSummary() {
        return summary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activity activity = (Activity) o;
        return Objects.equals(id, activity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id=" + id +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", summary=" + summary +
                '}';
    }

    public static class Builder {
        private Long id;
        private ActivityType type;
        private LocalDateTime timestamp;
        private ActivitySummary summary;

        private Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder type(ActivityType type) {
            this.type = type;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder summary(ActivitySummary summary) {
            this.summary = summary;
            return this;
        }

        public Activity build() {
            return new Activity(this);
        }
    }
}
