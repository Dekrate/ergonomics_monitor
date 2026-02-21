package pl.dekrate.ergonomicsmonitor.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "pl.dekrate.ergonomicsmonitor.repository")
public class DatabaseConfig {

    private final ObjectMapper objectMapper;

    public DatabaseConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new MapToJsonConverter(objectMapper));
        converters.add(new JsonToMapConverter(objectMapper));
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    @WritingConverter
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {
        private final ObjectMapper objectMapper;

        public MapToJsonConverter(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Json convert(Map<String, Object> source) {
            if (source == null) {
                return null;
            }
            try {
                return Json.of(objectMapper.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                throw new JsonConversionException("Error converting Map to JSON", e);
            }
        }
    }

    @ReadingConverter
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
        private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
        private final ObjectMapper objectMapper;

        public JsonToMapConverter(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Map<String, Object> convert(Json source) {
            if (source == null) {
                return null;
            }
            try {
                return objectMapper.readValue(source.asString(), MAP_TYPE);
            } catch (JsonProcessingException e) {
                throw new JsonConversionException("Error converting JSON to Map", e);
            }
        }
    }
}
