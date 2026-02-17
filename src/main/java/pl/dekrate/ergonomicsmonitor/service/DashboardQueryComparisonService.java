package pl.dekrate.ergonomicsmonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.model.DashboardMetricsEntity;
import pl.dekrate.ergonomicsmonitor.repository.DashboardMetricsRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service demonstrating WHEN to use declarative vs @Query methods.
 * This comparison shows real-world performance and readability considerations.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Service
public final class DashboardQueryComparisonService {

    private static final Logger log = LoggerFactory.getLogger(DashboardQueryComparisonService.class);

    private final DashboardMetricsRepository repository;

    public DashboardQueryComparisonService(DashboardMetricsRepository repository) {
        this.repository = repository;
    }

    /**
     * 🟢 PERFECT for declarative methods - simple, readable, optimal performance.
     * Spring generuje: SELECT * FROM dashboard_metrics WHERE user_id = ? AND metric_date = ?
     */
    public Mono<DashboardMetricsEntity> getMetricsForUserAndDate(UUID userId, LocalDate date) {
        log.debug("Using DECLARATIVE method for simple lookup: user={}, date={}", userId, date);

        // ✅ Deklaratywna metoda - czytelna, bezpieczna, wydajna
        return repository.findByUserIdAndMetricDate(userId, date)
                .doOnNext(metrics -> log.debug("Found metrics: {}", metrics.getId()))
                .doOnSubscribe(s -> log.debug("Executing declarative query"));
    }

    /**
     * 🟡 COULD BE declarative, but @Query gives database-specific control.
     * This shows when you might CHOOSE @Query for PostgreSQL optimizations.
     */
    public Flux<DashboardMetricsEntity> getRecentMetrics(UUID userId, int days) {
        log.debug("Using @Query for PostgreSQL INTERVAL function: user={}, days={}", userId, days);

        // 🔧 @Query dla database-specific funkcji (CURRENT_DATE - INTERVAL)
        return repository.findRecentByUserId(userId, days)
                .doOnNext(metrics -> log.debug("Recent metrics: {}", metrics.getMetricDate()))
                .doOnComplete(() -> log.debug("Completed recent metrics query"));
    }

    /**
     * 🔴 MUST BE @Query - aggregation functions impossible with declarative methods.
     */
    public Mono<Double> calculateAverageProductivity(UUID userId, LocalDate start, LocalDate end) {
        log.debug("Using @Query for AGGREGATION (AVG): user={}, range={} to {}", userId, start, end);

        // ❌ Niemożliwe deklaratywnie - Spring nie generuje funkcji agregujących
        return repository.calculateAverageProductivityScore(userId, start, end)
                .doOnNext(avg -> log.debug("Average productivity score: {}", avg))
                .doOnSuccess(avg -> log.info("Calculated average productivity: {} for user {}", avg, userId));
    }

    /**
     * 📊 PERFORMANCE COMPARISON - identical functionality, different approaches.
     * Shows that declarative methods have ZERO performance overhead.
     */
    public Mono<Void> performanceComparisonExample(UUID userId) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Opcja 1: Deklaratywna (Spring generuje SQL)
        Mono<Long> declarativeCount = repository.findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(
                userId, thirtyDaysAgo, LocalDate.now())
                .count()
                .doOnNext(count -> log.info("Declarative count: {}", count));

        // Opcja 2: @Query (ręczny SQL) - identyczna wydajność!
        Flux<DashboardMetricsEntity> queryResults = repository.findRecentByUserId(userId, 30);
        Mono<Long> queryCount = queryResults
                .count()
                .doOnNext(count -> log.info("@Query count: {}", count));

        // Obie metody mają identyczną wydajność - wybór zależy od czytelności i potrzeb
        return Mono.zip(declarativeCount, queryCount)
                .doOnNext(tuple -> {
                    log.info("Performance comparison - Declarative: {}, @Query: {}",
                            tuple.getT1(), tuple.getT2());
                    if (!tuple.getT1().equals(tuple.getT2())) {
                        log.warn("Count mismatch - possible data inconsistency");
                    }
                })
                .then();
    }

    /**
     * 🎯 BEST PRACTICES SUMMARY in action.
     */
    public Mono<String> demonstrateBestPractices(UUID userId) {
        return Mono.fromCallable(() -> {
            StringBuilder practices = new StringBuilder();
            practices.append("🎯 SPRING DATA R2DBC BEST PRACTICES:\n\n");

            practices.append("✅ USE DECLARATIVE for:\n");
            practices.append("  • Simple CRUD operations\n");
            practices.append("  • findByField, findByFieldAndOtherField\n");
            practices.append("  • Basic sorting: OrderByFieldDesc\n");
            practices.append("  • Date ranges: findByDateBetween\n");
            practices.append("  • Performance = identical to @Query!\n\n");

            practices.append("🔧 USE @QUERY for:\n");
            practices.append("  • Aggregations: COUNT, AVG, SUM, MAX\n");
            practices.append("  • Subqueries with EXISTS, NOT EXISTS\n");
            practices.append("  • Database-specific functions (PostgreSQL INTERVAL)\n");
            practices.append("  • Complex JOINs across multiple tables\n");
            practices.append("  • UPSERT operations (ON CONFLICT)\n");
            practices.append("  • Bulk operations (UPDATE, DELETE)\n\n");

            practices.append("⚡ PERFORMANCE NOTES:\n");
            practices.append("  • Declarative methods = zero overhead\n");
            practices.append("  • Spring generates optimal SQL automatically\n");
            practices.append("  • @Query gives control for complex scenarios\n");
            practices.append("  • Choose based on readability and requirements\n\n");

            practices.append("🧠 DECISION MATRIX:\n");
            practices.append("  • Can Spring generate it? → Use declarative\n");
            practices.append("  • Need aggregation/subquery? → Use @Query\n");
            practices.append("  • Need database-specific features? → Use @Query\n");
            practices.append("  • Performance-critical with specific SQL? → Use @Query\n");

            return practices.toString();
        })
        .doOnSuccess(summary -> log.info("Generated best practices summary for user: {}", userId));
    }
}
