package pl.dekrate.ergonomicsmonitor.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Value Object representing a break recommendation based on activity analysis. Immutable by design
 * following DDD principles.
 *
 * @author dekrate
 * @version 1.0
 */
public final class BreakRecommendation {

    private final Instant timestamp;
    private final BreakUrgency urgency;
    private final String reason;
    private final Duration suggestedBreakDuration;
    private final ActivityIntensityMetrics metrics;

    private BreakRecommendation(Builder builder) {
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.urgency = Objects.requireNonNull(builder.urgency, "urgency cannot be null");
        this.reason = Objects.requireNonNull(builder.reason, "reason cannot be null");
        this.suggestedBreakDuration =
                Objects.requireNonNull(
                        builder.suggestedBreakDuration, "suggestedBreakDuration cannot be null");
        this.metrics = Objects.requireNonNull(builder.metrics, "metrics cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public BreakUrgency getUrgency() {
        return urgency;
    }

    public String getReason() {
        return reason;
    }

    public Duration getSuggestedBreakDuration() {
        return suggestedBreakDuration;
    }

    public ActivityIntensityMetrics getMetrics() {
        return metrics;
    }

    /**
     * Convenience method to get break duration in minutes.
     *
     * @return duration in minutes
     */
    public int getDurationMinutes() {
        return (int) suggestedBreakDuration.toMinutes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakRecommendation that = (BreakRecommendation) o;
        return Objects.equals(timestamp, that.timestamp)
                && urgency == that.urgency
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, urgency, reason);
    }

    @Override
    public String toString() {
        return "BreakRecommendation{"
                + "timestamp="
                + timestamp
                + ", urgency="
                + urgency
                + ", reason='"
                + reason
                + '\''
                + ", suggestedBreakDuration="
                + suggestedBreakDuration
                + ", metrics="
                + metrics
                + '}';
    }

    public static class Builder {
        private Instant timestamp;
        private BreakUrgency urgency;
        private String reason;
        private Duration suggestedBreakDuration;
        private ActivityIntensityMetrics metrics;

        private Builder() {}

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder urgency(BreakUrgency urgency) {
            this.urgency = urgency;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder suggestedBreakDuration(Duration duration) {
            this.suggestedBreakDuration = duration;
            return this;
        }

        /**
         * Convenience method to set break duration in minutes.
         *
         * @param minutes duration in minutes
         * @return this builder
         */
        public Builder durationMinutes(int minutes) {
            this.suggestedBreakDuration = Duration.ofMinutes(minutes);
            return this;
        }

        public Builder metrics(ActivityIntensityMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public BreakRecommendation build() {
            return new BreakRecommendation(this);
        }
    }
}
