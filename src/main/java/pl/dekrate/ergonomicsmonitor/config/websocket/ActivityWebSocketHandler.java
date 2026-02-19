package pl.dekrate.ergonomicsmonitor.config.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time activity updates.
 * <p>
 * Manages WebSocket connections and streams activity data to connected clients.
 * Supports:
 * - Live activity event streaming
 * - Break recommendation notifications
 * - Connection management and cleanup
 * - JSON message serialization
 * <p>
 * Uses reactive streams for efficient real-time data delivery.
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Component
public final class ActivityWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ActivityWebSocketHandler.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Sinks.Many<ActivityUpdate> activitySink;
    private final Map<String, WebSocketSession> activeSessions;

    public ActivityWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.activitySink = Sinks.many().multicast().onBackpressureBuffer();
        this.activeSessions = new ConcurrentHashMap<>();
    }

    @Override
    @NonNull
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("WebSocket connection opened: {}", sessionId);

        // Store session for management
        activeSessions.put(sessionId, session);

        // Stream activity updates to client
        Mono<Void> outputMono = session.send(
            Flux.merge(
                // Welcome message first
                Flux.just(createWelcomeMessage()).take(1),
                // Then real-time updates
                activitySink.asFlux(),
                // Heartbeat to keep connection alive
                createHeartbeatStream()
            )
            .map(this::serializeMessage)
            .map(session::textMessage)
            .doOnError(error -> log.error("Error sending WebSocket message", error))
            .onErrorContinue((error, _) -> log.warn("Continuing after WebSocket error: {}", error.getMessage()))
        );

        // Handle incoming messages (if any)
        Mono<Void> inputMono = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(text -> handleIncomingMessage(sessionId, text))
                .doOnError(error -> log.error("Error receiving WebSocket message", error))
                .then();

        // Combine input and output, cleanup on completion
        return Mono.when(inputMono, outputMono)
                .doFinally(signalType -> {
                    log.info("WebSocket connection closed: {} ({})", sessionId, signalType);
                    activeSessions.remove(sessionId);
                });
    }

    /**
     * Broadcast activity update to all connected clients.
     */
    public void broadcastActivityUpdate(ActivityUpdate update) {
        log.debug("Broadcasting activity update: {}", update);

        Sinks.EmitResult result = activitySink.tryEmitNext(update);
        if (result.isFailure()) {
            log.warn("Failed to emit activity update: {}", result);
        }
    }

    /**
     * Get count of active WebSocket connections.
     */
    public int getActiveConnectionsCount() {
        return activeSessions.size();
    }

    private ActivityUpdate createWelcomeMessage() {
        return new ActivityUpdate(
            "WELCOME",
            "Connected to Ergonomics Monitor real-time stream",
            Instant.now(),
            Map.of(
                "connectionId", UUID.randomUUID().toString(),
                "serverTime", Instant.now().toString(),
                "features", "activity-events,break-recommendations,heartbeat"
            )
        );
    }

    private Flux<ActivityUpdate> createHeartbeatStream() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(tick -> new ActivityUpdate(
                    "HEARTBEAT",
                    "Server heartbeat",
                    Instant.now(),
                    Map.of("tick", tick, "activeConnections", getActiveConnectionsCount())
                ));
    }

    private void handleIncomingMessage(String sessionId, String message) {
        log.debug("Received message from {}: {}", sessionId, message);

        // Handle client messages here (e.g., subscription preferences)
        try {
            Map<String, Object> parsedMessage = objectMapper.readValue(message, MAP_TYPE);
            String type = (String) parsedMessage.get("type");

            switch (type) {
                case "PING" -> handlePing(sessionId);
                case "SUBSCRIBE" -> handleSubscription(sessionId, parsedMessage);
                case "UNSUBSCRIBE" -> handleUnsubscription(sessionId, parsedMessage);
                default -> log.debug("Unknown message type: {}", type);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse incoming WebSocket message: {}", message, e);
        }
    }

    private void handlePing(String sessionId) {
        // Respond to ping with pong
        ActivityUpdate pong = new ActivityUpdate(
            "PONG",
            "Pong response",
            Instant.now(),
            Map.of("sessionId", sessionId)
        );
        activitySink.tryEmitNext(pong);
    }

    private void handleSubscription(String sessionId, Map<String, Object> message) {
        String channel = (String) message.get("channel");
        log.info("Client {} subscribed to channel: {}", sessionId, channel);
        // Implement subscription logic here
    }

    private void handleUnsubscription(String sessionId, Map<String, Object> message) {
        String channel = (String) message.get("channel");
        log.info("Client {} unsubscribed from channel: {}", sessionId, channel);
        // Implement unsubscription logic here
    }

    private String serializeMessage(ActivityUpdate update) {
        try {
            return objectMapper.writeValueAsString(update);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize activity update", e);
            return "{\"type\":\"ERROR\",\"message\":\"Serialization failed\"}";
        }
    }

    /**
     * Data class for activity updates sent over WebSocket.
     */
    public record ActivityUpdate(
        String type,
        String message,
        Instant timestamp,
        Map<String, Object> data
    ) {}
}
