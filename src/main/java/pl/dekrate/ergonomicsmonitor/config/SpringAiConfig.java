package pl.dekrate.ergonomicsmonitor.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI configuration providing ChatClient bean.
 *
 * Configures ChatClient for AI-powered break analysis using Ollama.
 *
 * @author dekrate
 * @version 1.0
 */
@Configuration
public class SpringAiConfig {

    /**
     * Creates ChatClient bean for AI analysis.
     * Uses Ollama auto-configuration from spring-ai-ollama-spring-boot-starter.
     *
     * @param chatClientBuilder the auto-configured ChatClient builder
     * @return configured ChatClient instance
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
