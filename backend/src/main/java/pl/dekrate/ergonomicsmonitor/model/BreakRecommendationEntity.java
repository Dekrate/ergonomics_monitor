package pl.dekrate.ergonomicsmonitor.model;

import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing a break recommendation stored in the database. Maps to the {@code
 * break_recommendations} table.
 *
 * <p>This is separate from the {@link BreakRecommendation} value object to maintain clean
 * separation between domain model and persistence.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Table("break_recommendations")
public final class BreakRecommendationEntity {

    @Id private final UUID id;
    private final UUID userId;
    private final String urgency;

    private BreakRecommendationEntity(Builder builder) {
        this.id = builder.id;
        this.userId = Objects.requireNonNull(builder.userId, "userId cannot be null");
        this.urgency = Objects.requireNonNull(builder.urgency, "urgency cannot be null");
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
        return "BreakRecommendationEntity{"
                + "id="
                + id
                + ", userId="
                + userId
                + ", urgency='"
                + urgency
                + '\''
                + '}';
    }

    /**
     * Builder for {@link BreakRecommendationEntity} following the Builder pattern. Ensures
     * immutability and validation.
     */
    public static final class Builder {
        private UUID id;
        private UUID userId;
        private String urgency;

        private Builder() {}

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

        public BreakRecommendationEntity build() {
            return new BreakRecommendationEntity(this);
        }
    }
}
