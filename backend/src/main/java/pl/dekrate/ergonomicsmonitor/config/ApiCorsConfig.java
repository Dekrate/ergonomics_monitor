package pl.dekrate.ergonomicsmonitor.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Centralized CORS policy for backend APIs.
 *
 * <p>This avoids permissive wildcard origins and keeps cross-origin access explicitly allow-listed.
 */
@Configuration
public class ApiCorsConfig implements WebFluxConfigurer {

    private final List<String> allowedOrigins;

    public ApiCorsConfig(
            @Value("${ergonomics.security.allowed-origins:http://localhost:5173}")
                    List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
