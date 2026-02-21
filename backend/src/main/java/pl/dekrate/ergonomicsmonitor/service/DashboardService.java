package pl.dekrate.ergonomicsmonitor.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.config.websocket.ActivityWebSocketHandler;
import pl.dekrate.ergonomicsmonitor.model.DashboardMetricsEntity;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import pl.dekrate.ergonomicsmonitor.repository.DashboardMetricsRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for dashboard metrics generation and real-time streaming.
 *
 * <p>Provides comprehensive dashboard functionality including: - Metrics calculation and
 * aggregation - Real-time data streaming - Historical data analysis - Performance optimization
 * through caching
 *
 * <p>Implements SOLID principles with clear separation of concerns.
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

    public DashboardService(
            ActivityRepository activityRepository,
            DashboardMetricsRepository metricsRepository,
            ActivityWebSocketHandler webSocketHandler) {
        this.activityRepository = activityRepository;
        this.metricsRepository = metricsRepository;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Get or create daily metrics for a user and date. Uses caching strategy to avoid expensive
     * recalculations.
     */
    public Mono<DashboardMetricsEntity> getOrCreateDailyMetrics(UUID userId, LocalDate date) {
        log.debug("Getting daily metrics for user {} on {}", userId, date);

        return metricsRepository
                .findByUserIdAndMetricDate(userId, date)
                .switchIfEmpty(Mono.defer(() -> calculateAndSaveDailyMetrics(userId, date)))
                .doOnNext(metrics -> log.debug("Retrieved/created metrics: {}", metrics.getId()));
    }

    /** Get metrics for a date range. */
    public Flux<DashboardMetricsEntity> getMetricsInDateRange(
            UUID userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting metrics range: {} to {} for user {}", startDate, endDate, userId);

        return metricsRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(
                userId, startDate, endDate);
    }

    /** Generate weekly summary with aggregated statistics. */
    public Mono<Map<String, Object>> generateWeeklySummary(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6); // Last 7 days including today

        return getMetricsInDateRange(userId, weekStart, today)
                .collectList()
                .map(
                        weeklyMetrics -> {
                            long totalEvents =
                                    weeklyMetrics.stream()
                                            .mapToLong(DashboardMetricsEntity::getTotalEvents)
                                            .sum();

                            return Map.of(
                                    "period", "week",
                                    "startDate", weekStart,
                                    "endDate", today,
                                    "daysWithData", weeklyMetrics.size(),
                                    "totalEvents", totalEvents,
                                    "dailyMetrics", weeklyMetrics);
                        });
    }

    /** Generate monthly summary with trend analysis. */
    public Mono<Map<String, Object>> generateMonthlySummary(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        return getMetricsInDateRange(userId, monthStart, today)
                .collectList()
                .map(
                        monthlyMetrics ->
                                Map.of(
                                        "period", "month",
                                        "month", today.getMonth().toString(),
                                        "year", today.getYear(),
                                        "daysWithData", monthlyMetrics.size(),
                                        "monthlyMetrics", monthlyMetrics));
    }

    /** Create real-time dashboard update stream. */
    public Flux<Map<String, Object>> createRealTimeStream(UUID userId) {
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(_ -> generateRealTimeUpdate(userId))
                .doOnNext(
                        update ->
                                // Also broadcast via WebSocket
                                webSocketHandler.broadcastActivityUpdate(
                                        new ActivityWebSocketHandler.ActivityUpdate(
                                                "DASHBOARD_UPDATE",
                                                "Real-time dashboard metrics update",
                                                Instant.now(),
                                                update)));
    }

    /** Manually recalculate metrics for a user. */
    public Mono<Map<String, Object>> recalculateMetricsForUser(UUID userId) {
        LocalDate today = LocalDate.now();

        return calculateAndSaveDailyMetrics(userId, today)
                .map(
                        metrics ->
                                Map.of(
                                        "userId", userId,
                                        "date", today,
                                        "recalculatedAt", Instant.now(),
                                        "metricsId", metrics.getId(),
                                        "totalEvents", metrics.getTotalEvents()));
    }

    private Mono<DashboardMetricsEntity> calculateAndSaveDailyMetrics(UUID userId, LocalDate date) {
        log.debug("Calculating daily metrics for user {} on {}", userId, date);

        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return activityRepository
                .findByUserIdAndTimestampBetween(userId, startOfDay, endOfDay)
                .collectList()
                .map(events -> calculateMetricsFromEvents(userId, date, events))
                .flatMap(metricsRepository::upsert)
                .doOnNext(
                        metrics ->
                                log.info(
                                        "Calculated metrics for user {}: {} events",
                                        userId,
                                        metrics.getTotalEvents()));
    }

    private DashboardMetricsEntity calculateMetricsFromEvents(
            UUID userId, LocalDate date, java.util.List<ActivityEvent> events) {
        if (events.isEmpty()) {
            return createEmptyMetrics(userId, date);
        }

        long totalEvents = events.size();
        double avgIntensity =
                events.stream().mapToDouble(ActivityEvent::getIntensity).average().orElse(0.0);

        return DashboardMetricsEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .metricDate(date)
                .totalEvents(totalEvents)
                .avgIntensity(avgIntensity)
                .build();
    }

    private DashboardMetricsEntity createEmptyMetrics(UUID userId, LocalDate date) {
        return DashboardMetricsEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .metricDate(date)
                .totalEvents(0L)
                .avgIntensity(0.0)
                .build();
    }

    private Mono<Map<String, Object>> generateRealTimeUpdate(UUID userId) {
        return getOrCreateDailyMetrics(userId, LocalDate.now())
                .map(
                        metrics ->
                                Map.of(
                                        "type",
                                        "REAL_TIME_UPDATE",
                                        "timestamp",
                                        Instant.now(),
                                        "userId",
                                        userId,
                                        "currentMetrics",
                                        Map.of(
                                                "totalEvents", metrics.getTotalEvents(),
                                                "avgIntensity", metrics.getAvgIntensity())));
    }
}
