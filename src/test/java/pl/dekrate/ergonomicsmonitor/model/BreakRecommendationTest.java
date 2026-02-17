package pl.dekrate.ergonomicsmonitor.model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BreakRecommendation")
class BreakRecommendationTest {
    @Test
    @DisplayName("should build valid recommendation")
    void shouldBuildValid() {
        Instant now = Instant.now();
        BreakRecommendation rec = BreakRecommendation.builder()
                .timestamp(now).urgency(BreakUrgency.MEDIUM)
                .reason("Test").suggestedBreakDuration(Duration.ofMinutes(5))
                .metrics(createMetrics()).build();
        assertEquals(now, rec.getTimestamp());
    }
    @Test
    @DisplayName("should throw NPE on null")
    void shouldThrowNPE() {
        BreakRecommendation.Builder builder = BreakRecommendation.builder().timestamp(null);
        assertThrows(NullPointerException.class, builder::build);
    }
    private ActivityIntensityMetrics createMetrics() {
        return ActivityIntensityMetrics.builder()
                .totalEvents(100).timeWindow(Duration.ofMinutes(10)).build();
    }
}
