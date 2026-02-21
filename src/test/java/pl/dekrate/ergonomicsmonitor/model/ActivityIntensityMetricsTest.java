package pl.dekrate.ergonomicsmonitor.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for ActivityIntensityMetrics value object. Testing all business logic,
 * edge cases, and invariants.
 *
 * @author dekrate
 * @version 1.0
 */
@DisplayName("ActivityIntensityMetrics")
class ActivityIntensityMetricsTest {

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderTests {

        @Test
        @DisplayName("should build valid metrics with all fields")
        void shouldBuildValidMetrics() {
            // given
            long totalEvents = 150;
            long keyboardEvents = 100;
            long mouseEvents = 50;
            Duration timeWindow = Duration.ofMinutes(10);

            // when
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(totalEvents)
                            .keyboardEvents(keyboardEvents)
                            .mouseEvents(mouseEvents)
                            .timeWindow(timeWindow)
                            .build();

            // then
            assertAll(
                    "metrics properties",
                    () -> assertEquals(totalEvents, metrics.getTotalEvents()),
                    () -> assertEquals(keyboardEvents, metrics.getKeyboardEvents()),
                    () -> assertEquals(mouseEvents, metrics.getMouseEvents()),
                    () -> assertEquals(timeWindow, metrics.getTimeWindow()));
        }

        @Test
        @DisplayName("should throw NPE when timeWindow is null")
        void shouldThrowNPEWhenTimeWindowIsNull() {
            // given
            ActivityIntensityMetrics.Builder builder =
                    ActivityIntensityMetrics.builder().totalEvents(100).timeWindow(null);

            // when & then
            assertThrows(NullPointerException.class, builder::build);
        }
    }

    @Nested
    @DisplayName("Events Per Minute Calculation")
    class EventsPerMinuteTests {

        @Test
        @DisplayName("should calculate correct events per minute for 10-minute window")
        void shouldCalculateEventsPerMinuteCorrectly() {
            // given
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(150)
                            .keyboardEvents(100)
                            .mouseEvents(50)
                            .timeWindow(Duration.ofMinutes(10))
                            .build();

            // when
            double eventsPerMinute = metrics.getEventsPerMinute();

            // then
            assertEquals(15.0, eventsPerMinute, 0.01, "150 events / 10 minutes = 15 events/min");
        }

        @Test
        @DisplayName("should handle fractional minutes correctly")
        void shouldHandleFractionalMinutes() {
            // given - 90 seconds = 1.5 minutes
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(150)
                            .timeWindow(Duration.ofSeconds(90))
                            .build();

            // when
            double eventsPerMinute = metrics.getEventsPerMinute();

            // then
            assertEquals(100.0, eventsPerMinute, 0.01, "150 events / 1.5 minutes = 100 events/min");
        }

        @Test
        @DisplayName("should return 0 when timeWindow is zero")
        void shouldReturnZeroWhenTimeWindowIsZero() {
            // given
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(100)
                            .timeWindow(Duration.ZERO)
                            .build();

            // when
            double eventsPerMinute = metrics.getEventsPerMinute();

            // then
            assertEquals(0.0, eventsPerMinute, "Division by zero should be handled");
        }

        @Test
        @DisplayName("should return 0 when timeWindow is negative")
        void shouldReturnZeroWhenTimeWindowIsNegative() {
            // given
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(100)
                            .timeWindow(Duration.ofMinutes(-5))
                            .build();

            // when
            double eventsPerMinute = metrics.getEventsPerMinute();

            // then
            assertEquals(0.0, eventsPerMinute, "Negative duration should be handled");
        }
    }

    @Nested
    @DisplayName("Intensity Classification")
    class IntensityClassificationTests {

        @Test
        @DisplayName("should classify as intensive when >100 events/min")
        void shouldClassifyAsIntensive() {
            // given - 120 events/min
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(120)
                            .timeWindow(Duration.ofMinutes(1))
                            .build();

            // when & then
            assertTrue(metrics.isIntensive(), "120 events/min should be intensive");
            assertFalse(metrics.isCritical(), "120 events/min should not be critical");
        }

        @Test
        @DisplayName("should classify as critical when >200 events/min")
        void shouldClassifyAsCritical() {
            // given - 250 events/min
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(250)
                            .timeWindow(Duration.ofMinutes(1))
                            .build();

            // when & then
            assertTrue(metrics.isIntensive(), "250 events/min should be intensive");
            assertTrue(metrics.isCritical(), "250 events/min should be critical");
        }

        @Test
        @DisplayName("should not classify as intensive when <=100 events/min")
        void shouldNotClassifyAsIntensive() {
            // given - 100 events/min exactly
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(100)
                            .timeWindow(Duration.ofMinutes(1))
                            .build();

            // when & then
            assertFalse(metrics.isIntensive(), "100 events/min should not be intensive");
            assertFalse(metrics.isCritical(), "100 events/min should not be critical");
        }

        @Test
        @DisplayName("should handle boundary case at 100.1 events/min")
        void shouldHandleBoundaryCase() {
            // given - 1001 events in 10 minutes = 100.1 events/min
            ActivityIntensityMetrics metrics =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(1001)
                            .timeWindow(Duration.ofMinutes(10))
                            .build();

            // when & then
            assertTrue(metrics.isIntensive(), "100.1 events/min should be intensive");
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenFieldsMatch() {
            // given
            ActivityIntensityMetrics metrics1 = createSampleMetrics();
            ActivityIntensityMetrics metrics2 = createSampleMetrics();

            // when & then
            assertEquals(metrics1, metrics2);
            assertEquals(metrics1.hashCode(), metrics2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when total events differ")
        void shouldNotBeEqualWhenTotalEventsDiffer() {
            // given
            ActivityIntensityMetrics metrics1 =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(100)
                            .timeWindow(Duration.ofMinutes(10))
                            .build();

            ActivityIntensityMetrics metrics2 =
                    ActivityIntensityMetrics.builder()
                            .totalEvents(200)
                            .timeWindow(Duration.ofMinutes(10))
                            .build();

            // when & then
            assertNotEquals(metrics1, metrics2);
        }

        @Nested
        @DisplayName("toString Method")
        class ToStringTests {

            @Test
            @DisplayName("should include all relevant information")
            void shouldIncludeAllInformation() {
                // given
                ActivityIntensityMetrics metrics =
                        ActivityIntensityMetrics.builder()
                                .totalEvents(150)
                                .keyboardEvents(100)
                                .mouseEvents(50)
                                .timeWindow(Duration.ofMinutes(10))
                                .build();

                // when
                String result = metrics.toString();

                // then
                assertAll(
                        "toString contains all fields",
                        () -> assertTrue(result.contains("totalEvents=150")),
                        () -> assertTrue(result.contains("keyboardEvents=100")),
                        () -> assertTrue(result.contains("mouseEvents=50")),
                        () -> assertTrue(result.contains("eventsPerMinute=15")));
            }
        }

        // Helper method
        private ActivityIntensityMetrics createSampleMetrics() {
            return ActivityIntensityMetrics.builder()
                    .totalEvents(150)
                    .keyboardEvents(100)
                    .mouseEvents(50)
                    .timeWindow(Duration.ofMinutes(10))
                    .build();
        }
    }
}
