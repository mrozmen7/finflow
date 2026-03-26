package com.finflow.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a shared {@link ObjectMapper} bean with Java time support.
 * {@code @ConditionalOnMissingBean} ensures this does not conflict if
 * Spring Boot's JacksonAutoConfiguration registers one first.
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapper with {@link JavaTimeModule} registered so that
     * {@code LocalDateTime} and other java.time types serialise correctly to JSON.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
