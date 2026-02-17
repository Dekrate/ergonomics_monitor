package pl.dekrate.ergonomicsmonitor.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.ActivityIntensityMetrics;
import pl.dekrate.ergonomicsmonitor.model.BreakRecommendation;
import pl.dekrate.ergonomicsmonitor.model.BreakUrgency;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI-powered break recommendation strategy using Ollama LLM.
 *
 * Analyzes activity patterns using artificial intelligence to provide
 * intelligent and personalized break recommendations based on:
 * - Activity intensity patterns
 * - Work duration analysis
 * - Ergonomic risk assessment
 * - Personalized recommendations
 *
 * Implements Strategy pattern for pluggable AI analysis.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Component
public final class AIBreakRecommendationStrategy implements IntensityAnalysisStrategy {

    private static final Logger log = LoggerFactory.getLogger(AIBreakRecommendationStrategy.class);

    private static final String ANALYSIS_PROMPT = """
        Jesteś ekspertem ds. ergonomii pracy przy komputerze. Przeanalizuj poniższe dane aktywności użytkownika:
        
        Statystyki aktywności (ostatnie {timeWindow} minut):
        - Łączna liczba zdarzeń: {totalEvents}
        - Średnia intensywność: {avgIntensity}
        - Maksymalna intensywność: {maxIntensity}
        - Czas pracy: {workDurationMinutes} minut
        
        Wzorce aktywności:
        {activityPatterns}
        
        Na podstawie tych danych, oceń ryzyko problemów ergonomicznych i zaleć przerwę jeśli to konieczne.
        
        Odpowiedz TYLKO w formacie JSON:
        {{
            "needsBreak": true/false,
            "urgency": "LOW/MEDIUM/CRITICAL", 
            "durationMinutes": liczba_minut,
            "reason": "krótkie uzasadnienie"
        }}
        """;

    private final ChatClient chatClient;

    public AIBreakRecommendationStrategy(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Mono<BreakRecommendation> analyze(List<ActivityEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("No events to analyze with AI strategy");
            return Mono.empty();
        }

        return Mono.fromCallable(() -> prepareAnalysisData(events))
                .flatMap(this::queryAI)
                .flatMap(this::parseAIResponse)
                .doOnNext(rec -> log.info("AI recommendation: {} urgency, {} minutes break",
                    rec.getUrgency(), rec.getDurationMinutes()))
                .doOnError(error -> log.error("AI analysis failed", error))
                .onErrorResume(error -> {
                    log.warn("Falling back to heuristic analysis due to AI error: {}", error.getMessage());
                    return fallbackAnalysis(events);
                });
    }

    @Override
    public String getStrategyName() {
        return "AI-Powered Analysis (Ollama)";
    }

    private Map<String, Object> prepareAnalysisData(List<ActivityEvent> events) {
        Instant now = Instant.now();
        Instant earliest = events.stream()
                .map(ActivityEvent::getTimestamp)
                .min(Instant::compareTo)
                .orElse(now);

        long timeWindowMinutes = Duration.between(earliest, now).toMinutes();
        double avgIntensity = events.stream()
                .mapToDouble(ActivityEvent::getIntensity)
                .average()
                .orElse(0.0);

        double maxIntensity = events.stream()
                .mapToDouble(ActivityEvent::getIntensity)
                .max()
                .orElse(0.0);

        String patterns = events.stream()
                .map(event -> String.format("%.1f intensity at %s",
                    event.getIntensity(), event.getTimestamp()))
                .limit(10) // Limit to avoid token overflow
                .collect(Collectors.joining("\n- ", "- ", ""));

        return Map.of(
            "timeWindow", timeWindowMinutes,
            "totalEvents", events.size(),
            "avgIntensity", String.format("%.2f", avgIntensity),
            "maxIntensity", String.format("%.2f", maxIntensity),
            "workDurationMinutes", timeWindowMinutes,
            "activityPatterns", patterns
        );
    }

    private Mono<String> queryAI(Map<String, Object> analysisData) {
        return Mono.fromCallable(() -> {
            log.debug("Querying AI with data: {}", analysisData);

            PromptTemplate promptTemplate = new PromptTemplate(ANALYSIS_PROMPT);
            Prompt prompt = promptTemplate.create(analysisData);

            return chatClient.prompt(prompt)
                    .call()
                    .content();
        })
        .doOnNext(response -> log.debug("AI response: {}", response));
    }

    private Mono<BreakRecommendation> parseAIResponse(String aiResponse) {
        return Mono.fromCallable(() -> {
            try {
                // Simple JSON parsing - in production use Jackson ObjectMapper
                String cleaned = aiResponse.trim()
                        .replaceAll("```json", "")
                        .replaceAll("```", "")
                        .trim();

                log.debug("Parsing AI response: {}", cleaned);

                // Extract values using simple string parsing
                boolean needsBreak = cleaned.contains("\"needsBreak\": true");

                if (!needsBreak) {
                    return null; // No break needed
                }

                String urgencyStr = extractJsonValue(cleaned, "urgency");
                String durationStr = extractJsonValue(cleaned, "durationMinutes");
                String reason = extractJsonValue(cleaned, "reason");

                BreakUrgency urgency = parseUrgency(urgencyStr);
                int duration = Integer.parseInt(durationStr.replaceAll("[^0-9]", ""));

                return BreakRecommendation.builder()
                        .timestamp(Instant.now())
                        .urgency(urgency)
                        .durationMinutes(duration)
                        .reason("AI Analysis: " + reason)
                        .metrics(ActivityIntensityMetrics.builder()
                                .totalEvents(0)
                                .timeWindow(Duration.ofMinutes(duration))
                                .build())
                        .build();

            } catch (Exception e) {
                log.error("Failed to parse AI response: {}", aiResponse, e);
                throw new IllegalArgumentException("Invalid AI response format", e);
            }
        });
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\"?([^,\"}]+)\"?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);

        if (m.find()) {
            return m.group(1).trim().replaceAll("\"", "");
        }

        throw new IllegalArgumentException("Key not found: " + key);
    }

    private BreakUrgency parseUrgency(String urgencyStr) {
        try {
            return BreakUrgency.valueOf(urgencyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown urgency level: {}, defaulting to MEDIUM", urgencyStr);
            return BreakUrgency.MEDIUM;
        }
    }

    /**
     * Fallback analysis when AI fails - uses simple heuristics.
     */
    private Mono<BreakRecommendation> fallbackAnalysis(List<ActivityEvent> events) {
        double avgIntensity = events.stream()
                .mapToDouble(ActivityEvent::getIntensity)
                .average()
                .orElse(0.0);

        if (avgIntensity > 150.0) {
            return Mono.just(BreakRecommendation.builder()
                    .timestamp(Instant.now())
                    .urgency(BreakUrgency.CRITICAL)
                    .durationMinutes(10)
                    .reason("High intensity detected - fallback analysis")
                    .metrics(ActivityIntensityMetrics.builder()
                            .totalEvents(events.size())
                            .timeWindow(Duration.ofMinutes(10))
                            .build())
                    .build());
        } else if (avgIntensity > 75.0) {
            return Mono.just(BreakRecommendation.builder()
                    .timestamp(Instant.now())
                    .urgency(BreakUrgency.MEDIUM)
                    .durationMinutes(5)
                    .reason("Moderate intensity detected - fallback analysis")
                    .metrics(ActivityIntensityMetrics.builder()
                            .totalEvents(events.size())
                            .timeWindow(Duration.ofMinutes(5))
                            .build())
                    .build());
        }

        return Mono.empty();
    }
}
