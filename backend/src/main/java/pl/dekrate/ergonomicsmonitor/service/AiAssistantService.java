package pl.dekrate.ergonomicsmonitor.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import pl.dekrate.ergonomicsmonitor.ActivityEvent;
import pl.dekrate.ergonomicsmonitor.model.AiLanguage;
import pl.dekrate.ergonomicsmonitor.repository.ActivityRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * AI assistant service that answers user questions in the context of recent ergonomics activity.
 */
@Service
public class AiAssistantService {

    private static final String ASSISTANT_PROMPT_PL =
            """
            Jesteś asystentem ergonomii. Odpowiadasz po polsku.

            Kontekst aktywności użytkownika:
            %s

            Pytanie użytkownika:
            %s

            Odpowiedz krótko i konkretnie (maksymalnie 6 zdań).
            """;

    private static final String ASSISTANT_PROMPT_EN =
            """
            You are an ergonomics assistant. Respond in English.

            User activity context:
            %s

            User question:
            %s

            Keep your answer concise and actionable (maximum 6 sentences).
            """;

    private final ActivityRepository activityRepository;
    private final ChatClient chatClient;
    private final AiLanguagePreferenceService languagePreferenceService;

    public AiAssistantService(
            ActivityRepository activityRepository,
            ChatClient chatClient,
            AiLanguagePreferenceService languagePreferenceService) {
        this.activityRepository =
                Objects.requireNonNull(activityRepository, "activityRepository cannot be null");
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient cannot be null");
        this.languagePreferenceService =
                Objects.requireNonNull(
                        languagePreferenceService, "languagePreferenceService cannot be null");
    }

    /**
     * Answers a user question using recent activity context.
     *
     * @param userId active user id
     * @param question user question
     * @return map with answer and metadata
     */
    public Mono<Map<String, Object>> askQuestion(UUID userId, String question) {
        AiLanguage language = languagePreferenceService.getCurrentLanguage();

        return activityRepository
                .findLatest50EventsByUserId(userId)
                .collectList()
                .flatMap(events -> queryAssistant(events, question, language))
                .map(
                        answer ->
                                Map.of(
                                        "answer", answer,
                                        "language", language,
                                        "userId", userId,
                                        "timestamp", Instant.now()));
    }

    private Mono<String> queryAssistant(
            List<ActivityEvent> events, String question, AiLanguage language) {
        String context = buildContext(events, language);
        String prompt = buildPrompt(context, question, language);
        return Mono.fromCallable(() -> chatClient.prompt(prompt).call().content())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String buildPrompt(String context, String question, AiLanguage language) {
        return switch (language) {
            case PL -> ASSISTANT_PROMPT_PL.formatted(context, question);
            case EN -> ASSISTANT_PROMPT_EN.formatted(context, question);
        };
    }

    private String buildContext(List<ActivityEvent> events, AiLanguage language) {
        if (events.isEmpty()) {
            return switch (language) {
                case PL -> "Brak danych aktywności dla tego użytkownika.";
                case EN -> "No activity data available for this user.";
            };
        }

        return events.stream()
                .limit(20)
                .map(
                        event ->
                                String.format(
                                        "- %s | type=%s | intensity=%.2f | metadata=%s",
                                        event.getTimestamp(),
                                        event.getType(),
                                        event.getIntensity(),
                                        event.getMetadata()))
                .collect(Collectors.joining("\n"));
    }
}
