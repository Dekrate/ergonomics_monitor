package pl.dekrate.ergonomicsmonitor.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Value Object encapsulating metrics about activity intensity over a time period.
 * Immutable and contains derived calculations for ergonomic analysis.
 * <p>
 * All public methods are part of the Value Object API and are used by:
 * - Domain services (PomodoroIntensityStrategy)
 * - Notification adapters (WindowsNativeNotifier)
 * - Unit tests (ActivityIntensityMetricsTest)
 *
 * @author dekrate
 * @version 1.0
 */
//@SuppressWarnings("java:S1450") // Public API methods used by multiple components
public final class ActivityIntensityMetrics {

    private final long totalEvents;
    private final long keyboardEvents;
    private final long mouseEvents;
    private final Duration timeWindow;
    private final double eventsPerMinute;

    private ActivityIntensityMetrics(Builder builder) {
        this.totalEvents = builder.totalEvents;
        this.keyboardEvents = builder.keyboardEvents;
        this.mouseEvents = builder.mouseEvents;
        this.timeWindow = Objects.requireNonNull(builder.timeWindow, "timeWindow cannot be null");
        this.eventsPerMinute = calculateEventsPerMinute(totalEvents, timeWindow);
    }

    private static double calculateEventsPerMinute(long totalEvents, Duration timeWindow) {
        if (timeWindow.isZero() || timeWindow.isNegative()) {
            return 0.0;
        }
        double minutes = timeWindow.toSeconds() / 60.0;
        return totalEvents / minutes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public long getKeyboardEvents() {
        return keyboardEvents;
    }

    public long getMouseEvents() {
        return mouseEvents;
    }

    public Duration getTimeWindow() {
        return timeWindow;
    }

    public double getEventsPerMinute() {
        return eventsPerMinute;
    }

    /**
     * Determines if the activity level is considered intensive based on ergonomic guidelines.
     * Threshold: >100 events per minute is considered high intensity.
     */
    public boolean isIntensive() {
        return eventsPerMinute > 100.0;
    }

    /**
     * Determines if the activity level is critical (extremely high).
     * Threshold: >200 events per minute requires immediate attention.
     */
    public boolean isCritical() {
        return eventsPerMinute > 200.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityIntensityMetrics that = (ActivityIntensityMetrics) o;
        return totalEvents == that.totalEvents
            && keyboardEvents == that.keyboardEvents
            && mouseEvents == that.mouseEvents
            && Objects.equals(timeWindow, that.timeWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalEvents, keyboardEvents, mouseEvents, timeWindow);
    }

    @Override
    public String toString() {
        return "ActivityIntensityMetrics{" +
                "totalEvents=" + totalEvents +
                ", keyboardEvents=" + keyboardEvents +
                ", mouseEvents=" + mouseEvents +
                ", timeWindow=" + timeWindow +
                ", eventsPerMinute=" + String.format("%.2f", eventsPerMinute) +
                '}';
    }

    public static class Builder {
        private long totalEvents;
        private long keyboardEvents;
        private long mouseEvents;
        private Duration timeWindow;

        private Builder() {}

        public Builder totalEvents(long totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder keyboardEvents(long keyboardEvents) {
            this.keyboardEvents = keyboardEvents;
            return this;
        }

        public Builder mouseEvents(long mouseEvents) {
            this.mouseEvents = mouseEvents;
            return this;
        }

        public Builder timeWindow(Duration timeWindow) {
            this.timeWindow = timeWindow;
            return this;
        }

        public ActivityIntensityMetrics build() {
            return new ActivityIntensityMetrics(this);
        }
    }
}

