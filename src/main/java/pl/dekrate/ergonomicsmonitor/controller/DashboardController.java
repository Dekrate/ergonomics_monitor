package pl.dekrate.ergonomicsmonitor.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pl.dekrate.ergonomicsmonitor.config.websocket.ActivityWebSocketHandler;
import pl.dekrate.ergonomicsmonitor.model.DashboardMetricsEntity;
import pl.dekrate.ergonomicsmonitor.service.DashboardService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for dashboard functionality.
 * <p>
 * Provides comprehensive dashboard endpoints for:
 * - Real-time metrics streaming
 * - Historical data aggregation
 * - User activity analytics
 * - System health monitoring
 * <p>
 * Supports both traditional REST responses and Server-Sent Events (SSE)
 * for real-time dashboard updates.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", allowedHeaders = "*") // Configure properly for production
public final class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final ActivityWebSocketHandler webSocketHandler;

    public DashboardController(DashboardService dashboardService,
                              ActivityWebSocketHandler webSocketHandler) {
        this.dashboardService = dashboardService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Get dashboard metrics for a specific user and date.
     *
     * @param userId user identifier
     * @param date metric date (optional, defaults to today)
     * @return dashboard metrics for the specified date
     */
    @GetMapping("/metrics/{userId}")
    public Mono<DashboardMetricsEntity> getDailyMetrics(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.debug("Getting daily metrics for user {} on date {}", userId, targetDate);

        return dashboardService.getOrCreateDailyMetrics(userId, targetDate)
                .doOnNext(metrics -> log.debug("Retrieved metrics: {}", metrics.getId()));
    }

    /**
     * Get dashboard metrics for a date range.
     *
     * @param userId user identifier
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return flux of dashboard metrics for the date range
     */
    @GetMapping("/metrics/{userId}/range")
    public Flux<DashboardMetricsEntity> getMetricsRange(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("Getting metrics range for user {} from {} to {}", userId, startDate, endDate);

        return dashboardService.getMetricsInDateRange(userId, startDate, endDate)
                .doOnNext(metrics -> log.debug("Retrieved range metric: {}", metrics.getMetricDate()));
    }

    /**
     * Get current week dashboard summary.
     *
     * @param userId user identifier
     * @return weekly dashboard summary
     */
    @GetMapping("/summary/week/{userId}")
    public Mono<Map<String, Object>> getWeeklySummary(@PathVariable UUID userId) {
        log.debug("Getting weekly summary for user {}", userId);

        return dashboardService.generateWeeklySummary(userId)
                .doOnNext(summary -> log.debug("Generated weekly summary with {} metrics",
                    ((Map<?, ?>) summary.get("dailyMetrics")).size()));
    }

    /**
     * Get current month dashboard summary.
     *
     * @param userId user identifier
     * @return monthly dashboard summary
     */
    @GetMapping("/summary/month/{userId}")
    public Mono<Map<String, Object>> getMonthlySummary(@PathVariable UUID userId) {
        log.debug("Getting monthly summary for user {}", userId);

        return dashboardService.generateMonthlySummary(userId)
                .doOnNext(summary -> log.debug("Generated monthly summary"));
    }

    /**
     * Real-time dashboard updates via Server-Sent Events (SSE).
     *
     * @param userId user identifier
     * @return flux of real-time dashboard updates
     */
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> streamDashboardUpdates(@PathVariable UUID userId) {
        log.info("Starting dashboard stream for user {}", userId);

        return dashboardService.createRealTimeStream(userId)
                .doOnNext(update -> log.debug("Streaming dashboard update: {}", update.get("type")))
                .doOnCancel(() -> log.info("Dashboard stream cancelled for user {}", userId))
                .doOnError(error -> log.error("Dashboard stream error for user {}", userId, error));
    }

    /**
     * Trigger manual metrics recalculation for a user.
     *
     * @param userId user identifier
     * @return recalculation result
     */
    @PostMapping("/recalculate/{userId}")
    public Mono<Map<String, Object>> recalculateMetrics(@PathVariable UUID userId) {
        log.info("Manual metrics recalculation triggered for user {}", userId);

        return dashboardService.recalculateMetricsForUser(userId)
                .doOnNext(result -> log.info("Recalculation completed for user {}: {}", userId, result));
    }

    /**
     * Get system health and dashboard statistics.
     *
     * @return system health information
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> getSystemHealth() {
        log.debug("Getting system health information");

        return Mono.fromCallable(() -> Map.of(
            "activeWebSocketConnections", webSocketHandler.getActiveConnectionsCount(),
            "systemTime", System.currentTimeMillis(),
            "status", "healthy",
            "features", Map.of(
                "realTimeStreaming", true,
                "aiRecommendations", true,
                "dashboardMetrics", true,
                "webSocketSupport", true
            )
        ));
    }

    /**
     * Get dashboard configuration and capabilities.
     *
     * @return dashboard configuration
     */
    @GetMapping("/config")
    public Mono<Map<String, Object>> getDashboardConfig() {
        return Mono.just(Map.of(
            "features", Map.of(
                "realTimeUpdates", true,
                "historicalData", true,
                "aiAnalysis", true,
                "exportData", true
            ),
            "limits", Map.of(
                "maxDateRange", 365,
                "maxEventsPerQuery", 1000
            ),
            "endpoints", Map.of(
                "websocket", "/ws/activity",
                "stream", "/api/dashboard/stream/{userId}",
                "metrics", "/api/dashboard/metrics/{userId}"
            )
        ));
    }

    /**
     * Exception handler for dashboard-specific errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Dashboard request validation error: {}", e.getMessage());

        return Mono.just(Map.of(
            "error", "VALIDATION_ERROR",
            "message", e.getMessage(),
            "timestamp", System.currentTimeMillis()
        ));
    }
}
