package com.finflow.transaction.integration;

import com.finflow.transaction.domain.Account;
import com.finflow.transaction.domain.Transaction;
import com.finflow.transaction.domain.TransactionStatus;
import com.finflow.transaction.infrastructure.AccountRepository;
import com.finflow.transaction.infrastructure.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AccountIntegrationIT {

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
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("should save and retrieve account with all fields intact")
    void should_SaveAndRetrieveAccount() {
        Account account = new Account("Jane Doe", "CHF");
        Account saved = accountRepository.save(account);

        Account found = accountRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getOwnerName()).isEqualTo("Jane Doe");
        assertThat(found.getCurrency()).isEqualTo("CHF");
        assertThat(found.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should update balance correctly after deposit")
    void should_UpdateBalance_When_Deposit() {
        Account account = new Account("Jane Doe", "CHF");
        account = accountRepository.save(account);

        account.deposit(new BigDecimal("500.00"));
        accountRepository.save(account);

        Account updated = accountRepository.findById(account.getId()).orElseThrow();

        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("should persist transaction with correct status")
    void should_PersistTransactionStatus() {
        Account source = accountRepository.save(new Account("Source", "CHF"));
        Account target = accountRepository.save(new Account("Target", "CHF"));

        Transaction transaction = new Transaction(
            source.getId(), target.getId(),
            new BigDecimal("100.00"), "CHF", "Test payment", "SYSTEM"
        );
        transaction.complete();
        Transaction saved = transactionRepository.save(transaction);

        Transaction found = transactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(found.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(found.getCurrency()).isEqualTo("CHF");
        assertThat(found.getCreatedAt()).isNotNull();
    }
}
