package pl.dekrate.ergonomicsmonitor.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Konfiguracja Spring AI udostępniająca ziarno ChatClient.
 *
 * Konfiguruje ChatClient do analizy przerw zasilanej przez AI przy użyciu Ollama.
 * Ręcznie konfiguruje OllamaApi z wydłużonym czasem oczekiwania, aby wspierać lokalne modele LLM.
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.chat.model:SpeakLeash/bielik-11b-v3.0-instruct:bf16}")
    private String modelName;

    /**
     * Tworzy niestandardowy RestClient.Builder z wydłużonym timeoutem (5 minut).
     */
    @Bean
    public RestClient.Builder ollamaRestClientBuilder() {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(60))
                .withReadTimeout(Duration.ofMinutes(5)));

        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * Tworzy niestandardowy WebClient.Builder z wydłużonym timeoutem (5 minut).
     */
    @Bean
    public WebClient.Builder ollamaWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * Ręcznie konfiguruje OllamaApi przy użyciu niestandardowych builderów.
     */
    @Bean
    public OllamaApi ollamaApi(RestClient.Builder ollamaRestClientBuilder, WebClient.Builder ollamaWebClientBuilder) {
        return new OllamaApi(baseUrl, ollamaRestClientBuilder, ollamaWebClientBuilder);
    }

    /**
     * Tworzy OllamaChatModel przy użyciu niestandardowego OllamaApi.
     */
    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .withOllamaApi(ollamaApi)
                .withDefaultOptions(OllamaOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .build())
                .build();
    }

    /**
     * Tworzy ziarno ChatClient do analizy AI przy użyciu niestandardowego ChatModel.
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
