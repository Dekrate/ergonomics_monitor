package pl.dekrate.ergonomicsmonitor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a break recommendation stored in the database.
 * Maps to the {@code break_recommendations} table.
 *
 * This is separate from the {@link BreakRecommendation} value object
 * to maintain clean separation between domain model and persistence.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Table("break_recommendations")
public final class BreakRecommendationEntity {

    @Id
    private final UUID id;
    private final UUID userId;
    private final String urgency;
    private final Integer durationMinutes;
    private final String reason;
    private final Boolean aiGenerated;
    private final Instant createdAt;
    private final Instant acceptedAt;
    private final Instant dismissedAt;
    private final Map<String, Object> metadata;

    private BreakRecommendationEntity(Builder builder) {
        this.id = builder.id;
        this.userId = Objects.requireNonNull(builder.userId, "userId cannot be null");
        this.urgency = Objects.requireNonNull(builder.urgency, "urgency cannot be null");
        this.durationMinutes = Objects.requireNonNull(builder.durationMinutes, "durationMinutes cannot be null");
        this.reason = Objects.requireNonNull(builder.reason, "reason cannot be null");
        this.aiGenerated = Objects.requireNonNull(builder.aiGenerated, "aiGenerated cannot be null");
        this.createdAt = builder.createdAt;
        this.acceptedAt = builder.acceptedAt;
        this.dismissedAt = builder.dismissedAt;
        this.metadata = builder.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUrgency() {
        return urgency;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getReason() {
        return reason;
    }

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakRecommendationEntity that = (BreakRecommendationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BreakRecommendationEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", urgency='" + urgency + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", aiGenerated=" + aiGenerated +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Builder for {@link BreakRecommendationEntity} following the Builder pattern.
     * Ensures immutability and validation.
     */
    public static final class Builder {
        private UUID id;
        private UUID userId;
        private String urgency;
        private Integer durationMinutes;
        private String reason;
        private Boolean aiGenerated;
        private Instant createdAt;
        private Instant acceptedAt;
        private Instant dismissedAt;
        private Map<String, Object> metadata;

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder urgency(String urgency) {
            this.urgency = urgency;
            return this;
        }

        public Builder urgency(BreakUrgency urgency) {
            this.urgency = urgency != null ? urgency.name() : null;
            return this;
        }

        public Builder durationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder aiGenerated(Boolean aiGenerated) {
            this.aiGenerated = aiGenerated;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder acceptedAt(Instant acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public Builder dismissedAt(Instant dismissedAt) {
            this.dismissedAt = dismissedAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public BreakRecommendationEntity build() {
            return new BreakRecommendationEntity(this);
        }
    }
}
