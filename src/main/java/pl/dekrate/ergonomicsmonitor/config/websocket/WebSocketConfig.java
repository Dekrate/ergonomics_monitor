package pl.dekrate.ergonomicsmonitor.config.websocket;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket configuration for real-time activity updates.
 * <p>
 * Provides real-time streaming of activity events and break recommendations
 * to connected clients via WebSocket connections.
 * <p>
 * Features:
 * - Real-time activity event streaming
 * - Break recommendation notifications
 * - Connection management
 * - Configurable via application properties
 *
 * @author dekrate
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableWebFlux
@ConditionalOnProperty(prefix = "ergonomics.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig {

    /**
     * Handler mapping for WebSocket endpoints.
     * Maps URL paths to WebSocket handlers.
     *
     * @param activityWebSocketHandler handler for activity events
     * @return configured handler mapping
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping(ActivityWebSocketHandler activityWebSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/activity", activityWebSocketHandler);
        map.put("/ws/activity/**", activityWebSocketHandler); // Support for path parameters

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(map);
        handlerMapping.setOrder(-1); // High priority

        return handlerMapping;
    }
}
