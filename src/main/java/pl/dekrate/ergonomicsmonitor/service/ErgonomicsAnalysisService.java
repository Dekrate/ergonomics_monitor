package pl.dekrate.ergonomicsmonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Service responsible for analyzing user activity data using Artificial Intelligence.
 * It periodically fetches aggregated data from the database and consults a local 
 * LLM (Bielik via Ollama) to assess ergonomic risks and fatigue levels.
 * 
 * @author dekrate
 * @version 1.0
 */
@Service
public class ErgonomicsAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ErgonomicsAnalysisService.class);
    
    private final ActivityRepository repository;
    private final ChatClient chatClient;

    private static final String PROMPT_TEMPLATE = """
            You are an ergonomics expert. Analyze the following user activity data from the last few minutes:
            %s
            
            Based on the intensity and frequency of keyboard/mouse events, provide a brief (2 sentences) assessment:
            1. Is the user at risk of RSI (Repetitive Strain Injury) or fatigue?
            2. Should they take a break?
            Respond in English.
            """;

    /**
     * Constructs the analysis service.
     * 
     * @param repository the reactive repository for activity events
     * @param chatClientBuilder the builder for Spring AI ChatClient
     */
    public ErgonomicsAnalysisService(ActivityRepository repository, ChatClient.Builder chatClientBuilder) {
        this.repository = repository;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Scheduled task that runs every 5 minutes to perform AI-driven analysis.
     * It uses a reactive pipeline to gather data and non-blocking AI calls.
     * The processing is offloaded to Virtual Threads by the underlying framework.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void analyzeRecentActivity() {
        log.info("Starting scheduled ergonomic analysis via AI...");

        repository.findAll()
                .take(50) // Analyze last 50 aggregated events
                .collectList()
                .flatMap(events -> {
                    if (events.isEmpty()) {
                        return Mono.empty();
                    }

                    String dataSummary = events.stream()
                            .map(e -> String.format("Time: %s, Type: %s, Intensity: %.1f, Metadata: %s", 
                                    e.getTimestamp(), e.getType(), e.getIntensity(), e.getMetadata()))
                            .collect(Collectors.joining("\n"));

                    String prompt = PROMPT_TEMPLATE.formatted(dataSummary);

                    return Mono.fromCallable(() -> chatClient.prompt(prompt).call().content());
                })
                .subscribe(
                    result -> log.info("AI Analysis Result:\n{}", result),
                    error -> log.error("Failed to perform AI analysis", error)
                );
    }
}
