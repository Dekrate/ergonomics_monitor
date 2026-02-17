package pl.dekrate.ergonomicsmonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import pl.dekrate.ergonomicsmonitor.config.websocket.ActivityWebSocketHandler;
import pl.dekrate.ergonomicsmonitor.model.DashboardMetricsEntity;
import pl.dekrate.ergonomicsmonitor.repository.DashboardMetricsRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * Service for dashboard metrics generation and real-time streaming.
 * <p>
 * Provides comprehensive dashboard functionality including:
 * - Metrics calculation and aggregation
 * - Real-time data streaming
 * - Historical data analysis
 * - Performance optimization through caching
 * <p>
 * Implements SOLID principles with clear separation of concerns.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Service
public final class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ActivityRepository activityRepository;
    private final DashboardMetricsRepository metricsRepository;
    private final ActivityWebSocketHandler webSocketHandler;

    public DashboardService(ActivityRepository activityRepository,
                           DashboardMetricsRepository metricsRepository,
                           ActivityWebSocketHandler webSocketHandler) {
        this.activityRepository = activityRepository;
        this.metricsRepository = metricsRepository;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Get or create daily metrics for a user and date.
     * Uses caching strategy to avoid expensive recalculations.
     */
    public Mono<DashboardMetricsEntity> getOrCreateDailyMetrics(UUID userId, LocalDate date) {
        log.debug("Getting daily metrics for user {} on {}", userId, date);

        return metricsRepository.findByUserIdAndMetricDate(userId, date)
                .switchIfEmpty(Mono.defer(() -> calculateAndSaveDailyMetrics(userId, date)))
                .doOnNext(metrics -> log.debug("Retrieved/created metrics: {}", metrics.getId()));
    }

    /**
     * Get metrics for a date range.
     */
    public Flux<DashboardMetricsEntity> getMetricsInDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting metrics range: {} to {} for user {}", startDate, endDate, userId);

        return metricsRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(userId, startDate, endDate);
    }

    /**
     * Generate weekly summary with aggregated statistics.
     */
    public Mono<Map<String, Object>> generateWeeklySummary(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6); // Last 7 days including today

        return getMetricsInDateRange(userId, weekStart, today)
                .collectList()
                .map(weeklyMetrics -> {
                    double avgProductivity = weeklyMetrics.stream()
                            .filter(m -> m.getProductivityScore() != null)
                            .mapToDouble(DashboardMetricsEntity::getProductivityScore)
                            .average()
                            .orElse(0.0);

                    long totalEvents = weeklyMetrics.stream()
                            .mapToLong(DashboardMetricsEntity::getTotalEvents)
                            .sum();

                    int totalWorkMinutes = weeklyMetrics.stream()
                            .mapToInt(DashboardMetricsEntity::getWorkDurationMinutes)
                            .sum();

                    int totalBreakMinutes = weeklyMetrics.stream()
                            .mapToInt(DashboardMetricsEntity::getBreakDurationMinutes)
                            .sum();

                    return Map.of(
                        "period", "week",
                        "startDate", weekStart,
                        "endDate", today,
                        "daysWithData", weeklyMetrics.size(),
                        "averageProductivityScore", Math.round(avgProductivity * 100.0) / 100.0,
                        "totalEvents", totalEvents,
                        "totalWorkHours", Math.round(totalWorkMinutes / 60.0 * 100.0) / 100.0,
                        "totalBreakHours", Math.round(totalBreakMinutes / 60.0 * 100.0) / 100.0,
                        "dailyMetrics", weeklyMetrics
                    );
                });
    }

    /**
     * Generate monthly summary with trend analysis.
     */
    public Mono<Map<String, Object>> generateMonthlySummary(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        return getMetricsInDateRange(userId, monthStart, today)
                .collectList()
                .map(monthlyMetrics -> {
                    // Calculate trends and patterns
                    double avgProductivity = monthlyMetrics.stream()
                            .filter(m -> m.getProductivityScore() != null)
                            .mapToDouble(DashboardMetricsEntity::getProductivityScore)
                            .average()
                            .orElse(0.0);

                    // Find best and worst days
                    var bestDay = monthlyMetrics.stream()
                            .filter(m -> m.getProductivityScore() != null)
                            .max((m1, m2) -> Double.compare(m1.getProductivityScore(), m2.getProductivityScore()));

                    var worstDay = monthlyMetrics.stream()
                            .filter(m -> m.getProductivityScore() != null)
                            .min((m1, m2) -> Double.compare(m1.getProductivityScore(), m2.getProductivityScore()));

                    return Map.of(
                        "period", "month",
                        "month", today.getMonth().toString(),
                        "year", today.getYear(),
                        "daysWithData", monthlyMetrics.size(),
                        "averageProductivityScore", Math.round(avgProductivity * 100.0) / 100.0,
                        "bestDay", bestDay.map(m -> Map.of(
                            "date", m.getMetricDate(),
                            "score", m.getProductivityScore()
                        )).orElse(null),
                        "worstDay", worstDay.map(m -> Map.of(
                            "date", m.getMetricDate(),
                            "score", m.getProductivityScore()
                        )).orElse(null),
                        "monthlyMetrics", monthlyMetrics
                    );
                });
    }

    /**
     * Create real-time dashboard update stream.
     */
    public Flux<Map<String, Object>> createRealTimeStream(UUID userId) {
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> generateRealTimeUpdate(userId))
                .doOnNext(update -> {
                    // Also broadcast via WebSocket
                    webSocketHandler.broadcastActivityUpdate(
                        new ActivityWebSocketHandler.ActivityUpdate(
                            "DASHBOARD_UPDATE",
                            "Real-time dashboard metrics update",
                            Instant.now(),
                            update
                        )
                    );
                });
    }

    /**
     * Manually recalculate metrics for a user.
     */
    public Mono<Map<String, Object>> recalculateMetricsForUser(UUID userId) {
        LocalDate today = LocalDate.now();

        return calculateAndSaveDailyMetrics(userId, today)
                .map(metrics -> Map.of(
                    "userId", userId,
                    "date", today,
                    "recalculatedAt", Instant.now(),
                    "metricsId", metrics.getId(),
                    "totalEvents", metrics.getTotalEvents(),
                    "productivityScore", metrics.getProductivityScore()
                ));
    }

    private Mono<DashboardMetricsEntity> calculateAndSaveDailyMetrics(UUID userId, LocalDate date) {
        log.debug("Calculating daily metrics for user {} on {}", userId, date);

        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return activityRepository.findByUserIdAndTimestampBetween(userId, startOfDay, endOfDay)
                .collectList()
                .map(events -> calculateMetricsFromEvents(userId, date, events))
                .flatMap(metricsRepository::upsert)
                .doOnNext(metrics -> log.info("Calculated metrics for user {}: {} events, score {}",
                    userId, metrics.getTotalEvents(), metrics.getProductivityScore()));
    }

    private DashboardMetricsEntity calculateMetricsFromEvents(UUID userId, LocalDate date,
                                                            java.util.List<ActivityEvent> events) {
        if (events.isEmpty()) {
            return createEmptyMetrics(userId, date);
        }

        long totalEvents = events.size();
        double avgIntensity = events.stream()
                .mapToDouble(ActivityEvent::getIntensity)
                .average()
                .orElse(0.0);

        double maxIntensity = events.stream()
                .mapToDouble(ActivityEvent::getIntensity)
                .max()
                .orElse(0.0);

        // Calculate work duration (time between first and last event)
        var timeRange = events.stream()
                .map(ActivityEvent::getTimestamp)
                .mapToLong(Instant::toEpochMilli)
                .summaryStatistics();

        int workDurationMinutes = (int) Duration.ofMillis(timeRange.getMax() - timeRange.getMin()).toMinutes();

        // Simple productivity calculation (can be enhanced with ML)
        double productivityScore = calculateProductivityScore(avgIntensity, totalEvents, workDurationMinutes);

        return DashboardMetricsEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .metricDate(date)
                .totalEvents(totalEvents)
                .avgIntensity(avgIntensity)
                .maxIntensity(maxIntensity)
                .breakRecommendationsCount(0) // Will be updated by break service
                .workDurationMinutes(workDurationMinutes)
                .breakDurationMinutes(0) // Will be calculated from break events
                .productivityScore(productivityScore)
                .metadata(Map.of(
                    "calculatedAt", Instant.now().toString(),
                    "eventsAnalyzed", totalEvents,
                    "version", "1.0"
                ))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private DashboardMetricsEntity createEmptyMetrics(UUID userId, LocalDate date) {
        return DashboardMetricsEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .metricDate(date)
                .totalEvents(0L)
                .avgIntensity(0.0)
                .maxIntensity(0.0)
                .breakRecommendationsCount(0)
                .workDurationMinutes(0)
                .breakDurationMinutes(0)
                .productivityScore(0.0)
                .metadata(Map.of("empty", true, "reason", "No events found"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private double calculateProductivityScore(double avgIntensity, long totalEvents, int workDurationMinutes) {
        if (workDurationMinutes == 0) return 0.0;

        // Normalize metrics to 0-100 scale
        double intensityScore = Math.min(avgIntensity / 100.0 * 40, 40); // Max 40 points
        double activityScore = Math.min(totalEvents / 500.0 * 40, 40); // Max 40 points
        double durationScore = Math.min(workDurationMinutes / 480.0 * 20, 20); // Max 20 points (8h = full score)

        return Math.round((intensityScore + activityScore + durationScore) * 100.0) / 100.0;
    }

    private Mono<Map<String, Object>> generateRealTimeUpdate(UUID userId) {
        return getOrCreateDailyMetrics(userId, LocalDate.now())
                .map(metrics -> Map.of(
                    "type", "REAL_TIME_UPDATE",
                    "timestamp", Instant.now(),
                    "userId", userId,
                    "currentMetrics", Map.of(
                        "totalEvents", metrics.getTotalEvents(),
                        "avgIntensity", metrics.getAvgIntensity(),
                        "productivityScore", metrics.getProductivityScore(),
                        "workHours", Math.round(metrics.getWorkDurationMinutes() / 60.0 * 100.0) / 100.0
                    )
                ));
    }
}


