package com.finflow.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 * Adds a global Bearer JWT security scheme so every endpoint shows the Authorize button.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "FinFlow Payment API",
        description = "Event-driven payment processing and fraud detection system",
        version = "0.3.0",
        contact = @Contact(
            name = "FinFlow Team",
            email = "dev@finflow.com"
        )
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {

    /**
     * Applies the Bearer JWT security requirement globally to all endpoints.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
