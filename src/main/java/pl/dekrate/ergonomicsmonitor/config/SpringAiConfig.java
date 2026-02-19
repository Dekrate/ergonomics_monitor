package pl.dekrate.ergonomicsmonitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Konfiguracja Spring AI udostępniająca ziarno ChatClient.
 * Wymusza długi czas oczekiwania (5 min) dla lokalnych modeli LLM.
 */
@Configuration
public class SpringAiConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConfig.class);

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.chat.model:SpeakLeash/bielik-11b-v3.0-instruct:bf16}")
    private String modelName;

    /**
     * Konfiguruje współdzielony HttpClient z ekstremalnie długim timeoutem.
     */
    private HttpClient getHttpClient() {
        return HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000);
    }

    @Bean
    @Primary
    public RestClient.Builder ollamaRestClientBuilder() {
        log.info("Inicjalizacja RestClient.Builder z timeoutem 5 minut dla modelu: {}", modelName);
        return RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(getHttpClient()));
    }

    @Bean
    @Primary
    public WebClient.Builder ollamaWebClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(getHttpClient()));
    }

    @Bean
    @Primary
    public OllamaApi ollamaApi(RestClient.Builder ollamaRestClientBuilder, WebClient.Builder ollamaWebClientBuilder) {
        return new OllamaApi(baseUrl, ollamaRestClientBuilder, ollamaWebClientBuilder);
    }

    @Bean
    @Primary
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .withOllamaApi(ollamaApi)
                .withDefaultOptions(OllamaOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .build())
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(OllamaChatModel chatModel) {
        log.info("Utworzono ChatClient z modelem: {}", modelName);
        return ChatClient.builder(chatModel).build();
    }
}
