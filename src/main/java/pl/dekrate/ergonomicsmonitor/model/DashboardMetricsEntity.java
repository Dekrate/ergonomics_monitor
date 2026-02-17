package pl.dekrate.ergonomicsmonitor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing pre-aggregated dashboard metrics for performance optimization.
 * Maps to the {@code dashboard_metrics} table.
 *
 * Stores daily aggregated statistics to avoid expensive real-time calculations
 * across large datasets of activity events.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Table("dashboard_metrics")
public final class DashboardMetricsEntity {

    @Id
    private final UUID id;
    private final UUID userId;
    private final LocalDate metricDate;
    private final Long totalEvents;
    private final Double avgIntensity;
    private final Double maxIntensity;
    private final Integer breakRecommendationsCount;
    private final Integer workDurationMinutes;
    private final Integer breakDurationMinutes;
    private final Double productivityScore;
    private final Map<String, Object> metadata;
    private final Instant createdAt;
    private final Instant updatedAt;

    private DashboardMetricsEntity(Builder builder) {
        this.id = builder.id;
        this.userId = Objects.requireNonNull(builder.userId, "userId cannot be null");
        this.metricDate = Objects.requireNonNull(builder.metricDate, "metricDate cannot be null");
        this.totalEvents = Objects.requireNonNull(builder.totalEvents, "totalEvents cannot be null");
        this.avgIntensity = Objects.requireNonNull(builder.avgIntensity, "avgIntensity cannot be null");
        this.maxIntensity = Objects.requireNonNull(builder.maxIntensity, "maxIntensity cannot be null");
        this.breakRecommendationsCount = Objects.requireNonNull(builder.breakRecommendationsCount, "breakRecommendationsCount cannot be null");
        this.workDurationMinutes = Objects.requireNonNull(builder.workDurationMinutes, "workDurationMinutes cannot be null");
        this.breakDurationMinutes = Objects.requireNonNull(builder.breakDurationMinutes, "breakDurationMinutes cannot be null");
        this.productivityScore = builder.productivityScore;
        this.metadata = builder.metadata;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
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

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public Long getTotalEvents() {
        return totalEvents;
    }

    public Double getAvgIntensity() {
        return avgIntensity;
    }

    public Double getMaxIntensity() {
        return maxIntensity;
    }

    public Integer getBreakRecommendationsCount() {
        return breakRecommendationsCount;
    }

    public Integer getWorkDurationMinutes() {
        return workDurationMinutes;
    }

    public Integer getBreakDurationMinutes() {
        return breakDurationMinutes;
    }

    public Double getProductivityScore() {
        return productivityScore;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardMetricsEntity that = (DashboardMetricsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DashboardMetricsEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", metricDate=" + metricDate +
                ", totalEvents=" + totalEvents +
                ", avgIntensity=" + avgIntensity +
                ", productivityScore=" + productivityScore +
                '}';
    }

    /**
     * Builder for {@link DashboardMetricsEntity} following the Builder pattern.
     * Ensures immutability and validation.
     */
    public static final class Builder {
        private UUID id;
        private UUID userId;
        private LocalDate metricDate;
        private Long totalEvents;
        private Double avgIntensity;
        private Double maxIntensity;
        private Integer breakRecommendationsCount;
        private Integer workDurationMinutes;
        private Integer breakDurationMinutes;
        private Double productivityScore;
        private Map<String, Object> metadata;
        private Instant createdAt;
        private Instant updatedAt;

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

        public Builder metricDate(LocalDate metricDate) {
            this.metricDate = metricDate;
            return this;
        }

        public Builder totalEvents(Long totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder avgIntensity(Double avgIntensity) {
            this.avgIntensity = avgIntensity;
            return this;
        }

        public Builder maxIntensity(Double maxIntensity) {
            this.maxIntensity = maxIntensity;
            return this;
        }

        public Builder breakRecommendationsCount(Integer breakRecommendationsCount) {
            this.breakRecommendationsCount = breakRecommendationsCount;
            return this;
        }

        public Builder workDurationMinutes(Integer workDurationMinutes) {
            this.workDurationMinutes = workDurationMinutes;
            return this;
        }

        public Builder breakDurationMinutes(Integer breakDurationMinutes) {
            this.breakDurationMinutes = breakDurationMinutes;
            return this;
        }

        public Builder productivityScore(Double productivityScore) {
            this.productivityScore = productivityScore;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public DashboardMetricsEntity build() {
            return new DashboardMetricsEntity(this);
        }
    }
}
