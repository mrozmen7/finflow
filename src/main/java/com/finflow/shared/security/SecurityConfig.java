package com.finflow.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.shared.config.CorrelationIdFilter;
import com.finflow.shared.config.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URI;
import java.time.Instant;

/**
 * Spring Security filter chain configuration.
 * Stateless JWT-based authentication; no CSRF (API-only project).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {
        "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/api-docs"
    };

    private static final String[] ACTUATOR_PATHS = {
        "/actuator", "/actuator/**"
    };

    private final JwtService jwtService;
    private final CorrelationIdFilter correlationIdFilter;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SecurityConfig(JwtService jwtService, CorrelationIdFilter correlationIdFilter) {
        this.jwtService = jwtService;
        this.correlationIdFilter = correlationIdFilter;
    }

    /**
     * Configures the security filter chain with JWT authentication.
     * Public endpoints: POST /accounts (account creation), /auth/**, Swagger UI, /actuator/**.
     * All other /api/** endpoints require a valid Bearer token.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SWAGGER_PATHS).permitAll()
                .requestMatchers(ACTUATOR_PATHS).permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/accounts").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED, "Authentication required");
                    problem.setTitle("Unauthorized");
                    problem.setType(URI.create("https://finflow.com/errors/unauthorized"));
                    problem.setProperty("timestamp", Instant.now());
                    objectMapper.writeValue(response.getWriter(), problem);
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN, "Access denied");
                    problem.setTitle("Forbidden");
                    problem.setType(URI.create("https://finflow.com/errors/forbidden"));
                    problem.setProperty("timestamp", Instant.now());
                    objectMapper.writeValue(response.getWriter(), problem);
                })
            )
            .build();
    }
}
