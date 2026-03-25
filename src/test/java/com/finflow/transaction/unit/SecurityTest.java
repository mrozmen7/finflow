package com.finflow.transaction.unit;

import com.finflow.shared.security.JwtService;
import com.finflow.transaction.application.AccountService;
import com.finflow.transaction.domain.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Spring Security filter chain: valid JWT → 200, missing token → 401.
 * Uses a full Spring context (Testcontainers for Flyway/JPA) with AccountService mocked
 * to prevent database interaction in the controller.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class SecurityTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("finflow_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("should return 200 when request carries a valid JWT token")
    void should_Return200_When_RequestHasValidToken() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccount(any(UUID.class))).thenReturn(new Account("Test Owner", "CHF"));

        String token = jwtService.generateToken("admin");

        mockMvc.perform(get("/accounts/" + accountId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 401 when request has no authorization token")
    void should_Return401_When_RequestHasNoToken() throws Exception {
        mockMvc.perform(get("/accounts/" + UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }
}
