package pl.dekrate.ergonomicsmonitor.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing pre-aggregated dashboard metrics for performance optimization. Maps to the
 * {@code dashboard_metrics} table.
 *
 * <p>Stores daily aggregated statistics to avoid expensive real-time calculations across large
 * datasets of activity events.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Table("dashboard_metrics")
public final class DashboardMetricsEntity {

    @Id private final UUID id;
    private final UUID userId;
    private final LocalDate metricDate;
    private final Long totalEvents;
    private final Double avgIntensity;

    @PersistenceCreator
    public DashboardMetricsEntity(
            UUID id, UUID userId, LocalDate metricDate, Long totalEvents, Double avgIntensity) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.metricDate = Objects.requireNonNull(metricDate, "metricDate cannot be null");
        this.totalEvents = Objects.requireNonNull(totalEvents, "totalEvents cannot be null");
        this.avgIntensity = Objects.requireNonNull(avgIntensity, "avgIntensity cannot be null");
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
        return "DashboardMetricsEntity{"
                + "id="
                + id
                + ", userId="
                + userId
                + ", metricDate="
                + metricDate
                + ", totalEvents="
                + totalEvents
                + ", avgIntensity="
                + avgIntensity
                + '}';
    }

    /**
     * Builder for {@link DashboardMetricsEntity} following the Builder pattern. Ensures
     * immutability and validation.
     */
    public static final class Builder {
        private UUID id;
        private UUID userId;
        private LocalDate metricDate;
        private Long totalEvents;
        private Double avgIntensity;

        private Builder() {}

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

        public DashboardMetricsEntity build() {
            return new DashboardMetricsEntity(id, userId, metricDate, totalEvents, avgIntensity);
        }
    }
}
