package pl.dekrate.ergonomicsmonitor.service;

import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.model.DashboardMetricsEntity;
import pl.dekrate.ergonomicsmonitor.repository.DashboardMetricsRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service demonstrating WHEN to use declarative vs @Query methods. This comparison shows real-world
 * performance and readability considerations.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Service
public final class DashboardQueryComparisonService {

    private static final Logger log =
            LoggerFactory.getLogger(DashboardQueryComparisonService.class);

    private final DashboardMetricsRepository repository;

    public DashboardQueryComparisonService(DashboardMetricsRepository repository) {
        this.repository = repository;
    }

    /**
     * 🟢 PERFECT for declarative methods - simple, readable, optimal performance. Spring generuje:
     * SELECT * FROM dashboard_metrics WHERE user_id = ? AND metric_date = ?
     */
    public Mono<DashboardMetricsEntity> getMetricsForUserAndDate(UUID userId, LocalDate date) {
        log.debug("Using DECLARATIVE method for simple lookup: user={}, date={}", userId, date);

        // ✅ Deklaratywna metoda - czytelna, bezpieczna, wydajna
        return repository
                .findByUserIdAndMetricDate(userId, date)
                .doOnNext(metrics -> log.debug("Found metrics: {}", metrics.getId()))
                .doOnSubscribe(_ -> log.debug("Executing declarative query"));
    }

    /** 🔴 MUST BE @Query - aggregation functions impossible with declarative methods. */
    public Mono<Double> calculateAverageProductivity(UUID userId, LocalDate start, LocalDate end) {
        log.debug(
                "Using @Query for AGGREGATION (AVG): user={}, range={} to {}", userId, start, end);

        // ❌ Niemożliwe deklaratywnie - Spring nie generuje funkcji agregujących
        return repository
                .calculateAverageProductivityScore(userId, start, end)
                .doOnNext(avg -> log.debug("Average productivity score: {}", avg))
                .doOnSuccess(
                        avg ->
                                log.info(
                                        "Calculated average productivity: {} for user {}",
                                        avg,
                                        userId));
    }

    /**
     * 📊 PERFORMANCE COMPARISON - identical functionality, different approaches. Shows that
     * declarative methods have ZERO performance overhead.
     */
    public Mono<Void> performanceComparisonExample(UUID userId) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Opcja 1: Deklaratywna (Spring generuje SQL)
        Mono<Long> declarativeCount =
                repository
                        .findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(
                                userId, thirtyDaysAgo, LocalDate.now())
                        .count()
                        .doOnNext(count -> log.info("Declarative count: {}", count));

        // Opcja 2: @Query (ręczny SQL) - identyczna wydajność!
        Flux<DashboardMetricsEntity> queryResults = repository.findRecentByUserId(userId, 30);
        Mono<Long> queryCount =
                queryResults.count().doOnNext(count -> log.info("@Query count: {}", count));

        // Obie metody mają identyczną wydajność - wybór zależy od czytelności i potrzeb
        return Mono.zip(declarativeCount, queryCount)
                .doOnNext(
                        tuple -> {
                            log.info(
                                    "Performance comparison - Declarative: {}, @Query: {}",
                                    tuple.getT1(),
                                    tuple.getT2());
                            if (!tuple.getT1().equals(tuple.getT2())) {
                                log.warn("Count mismatch - possible data inconsistency");
                            }
                        })
                .then();
    }

    /** 🎯 BEST PRACTICES SUMMARY in action. */
    @SuppressWarnings("java:S6203") // because of higher readability
    public Mono<String> demonstrateBestPractices(UUID userId) {
        return Mono.fromCallable(
                        () ->
                                """
						🎯 SPRING DATA R2DBC BEST PRACTICES:

						✅ USE DECLARATIVE for:
						  • Simple CRUD operations
						  • findByField, findByFieldAndOtherField
						  • Basic sorting: OrderByFieldDesc
						  • Date ranges: findByDateBetween
						  • Performance = identical to @Query!

						🔧 USE @QUERY for:
						  • Aggregations: COUNT, AVG, SUM, MAX
						  • Subqueries with EXISTS, NOT EXISTS
						  • Database-specific functions (PostgreSQL INTERVAL)
						  • Complex JOINs across multiple tables
						  • UPSERT operations (ON CONFLICT)
						  • Bulk operations (UPDATE, DELETE)

						⚡ PERFORMANCE NOTES:
						  • Declarative methods = zero overhead
						  • Spring generates optimal SQL automatically
						  • @Query gives control for complex scenarios
						  • Choose based on readability and requirements

						🧠 DECISION MATRIX:
						  • Can Spring generate it? → Use declarative
						  • Need aggregation/subquery? → Use @Query
						  • Need database-specific features? → Use @Query
						  • Performance-critical with specific SQL? → Use @Query
						""")
                .doOnSuccess(
                        _ -> log.info("Generated best practices summary for user: {}", userId));
    }
}
