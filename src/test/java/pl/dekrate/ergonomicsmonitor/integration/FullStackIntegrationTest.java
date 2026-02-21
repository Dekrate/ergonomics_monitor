package pl.dekrate.ergonomicsmonitor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.dekrate.ergonomicsmonitor.config.websocket.ActivityWebSocketHandler;
import pl.dekrate.ergonomicsmonitor.model.ActivityIntensityMetrics;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import pl.dekrate.ergonomicsmonitor.service.DashboardService;
import pl.dekrate.ergonomicsmonitor.service.strategy.AIBreakRecommendationStrategy;
import reactor.test.StepVerifier;

/**
 * Integration test for the complete AI + Dashboard + WebSocket functionality.
 *
 * <p>Tests the full stack integration without external dependencies, focusing on component
 * interaction and business logic validation.
 *
 * @author dekrate
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Disabled due to known npipe:// character issue in Testcontainers on Windows")
class FullStackIntegrationTest {

    @Autowired private DashboardService dashboardService;

    @Autowired private ActivityWebSocketHandler webSocketHandler;

    @Autowired private AIBreakRecommendationStrategy aiStrategy;

    @Test
    void shouldCreateDashboardMetricsSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        // When & Then
        dashboardService
                .getOrCreateDailyMetrics(userId, today)
                .as(StepVerifier::create)
                .assertNext(
                        metrics -> {
                            assertThat(metrics.getUserId()).isEqualTo(userId);
                            assertThat(metrics.getMetricDate()).isEqualTo(today);
                            assertThat(metrics.getTotalEvents()).isGreaterThanOrEqualTo(0);
                        })
                .verifyComplete();
    }

    @Test
    void shouldGenerateWeeklySummaryWithCorrectStructure() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        dashboardService
                .generateWeeklySummary(userId)
                .as(StepVerifier::create)
                .assertNext(
                        summary -> {
                            assertThat(summary)
                                    .containsKeys(
                                            "period",
                                            "startDate",
                                            "endDate",
                                            "daysWithData",
                                            "totalEvents",
                                            "dailyMetrics");
                            assertThat(summary.get("period")).isEqualTo("week");
                        })
                .verifyComplete();
    }

    @Test
    void shouldHaveWorkingWebSocketHandler() {
        // Given & When
        int initialConnections = webSocketHandler.getActiveConnectionsCount();

        // Simulate broadcasting an update
        webSocketHandler.broadcastActivityUpdate(
                new ActivityWebSocketHandler.ActivityUpdate(
                        "TEST",
                        "Test message",
                        java.time.Instant.now(),
                        java.util.Map.of("test", true)));

        // Then
        assertThat(initialConnections).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldHandleAIStrategyGracefully() {
        // Given - empty event list (AI will fail gracefully)
        java.util.List<pl.dekrate.ergonomicsmonitor.ActivityEvent> emptyEvents =
                java.util.Collections.emptyList();

        // When & Then
        aiStrategy
                .analyze(emptyEvents)
                .as(StepVerifier::create)
                .verifyComplete(); // Should complete without error
    }

    @Test
    void shouldIntegrateAllComponentsInRealTimeUpdate() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then - test real-time stream creation
        dashboardService
                .createRealTimeStream(userId)
                .take(1) // Take only first update to avoid infinite stream
                .as(StepVerifier::create)
                .assertNext(
                        update -> {
                            assertThat(update)
                                    .containsKeys("type", "timestamp", "userId", "currentMetrics");
                            assertThat(update.get("type")).isEqualTo("REAL_TIME_UPDATE");
                            assertThat(update.get("userId")).isEqualTo(userId);
                        })
                .verifyComplete();
    }

    @Test
    void shouldValidateAIStrategyName() {
        // When & Then
        String strategyName = aiStrategy.getStrategyName();
        assertThat(strategyName).isEqualTo("AI-Powered Analysis (Ollama)");
    }

    @Test
    void shouldCreateValidBreakRecommendation() {
        // Given & When
        BreakRecommendation recommendation =
                BreakRecommendation.builder()
                        .timestamp(Instant.now())
                        .urgency(BreakUrgency.MEDIUM)
                        .durationMinutes(5)
                        .reason("Integration test recommendation")
                        .metrics(
                                ActivityIntensityMetrics.builder()
                                        .totalEvents(100)
                                        .timeWindow(Duration.ofMinutes(5))
                                        .build())
                        .build();

        // Then
        assertThat(recommendation.getUrgency()).isEqualTo(BreakUrgency.MEDIUM);
        assertThat(recommendation.getDurationMinutes()).isEqualTo(5);
        assertThat(recommendation.getReason()).contains("Integration test");
    }

    @Test
    void shouldRecalculateMetricsSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        dashboardService
                .recalculateMetricsForUser(userId)
                .as(StepVerifier::create)
                .assertNext(
                        result -> {
                            assertThat(result)
                                    .containsKeys(
                                            "userId",
                                            "date",
                                            "recalculatedAt",
                                            "metricsId",
                                            "totalEvents",
                                            "productivityScore");
                            assertThat(result.get("userId")).isEqualTo(userId);
                        })
                .verifyComplete();
    }
}
