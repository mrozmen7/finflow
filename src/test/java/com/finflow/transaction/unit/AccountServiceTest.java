package com.finflow.transaction.unit;

import com.finflow.shared.exception.ResourceNotFoundException;
import com.finflow.transaction.application.AccountService;
import com.finflow.transaction.domain.Account;
import com.finflow.transaction.infrastructure.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account("John Doe", "CHF");
    }

    @Test
    @DisplayName("should create account when valid input is provided")
    void should_CreateAccount_When_ValidInput() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account result = accountService.createAccount("John Doe", "CHF");

        assertThat(result).isNotNull();
        assertThat(result.getOwnerName()).isEqualTo("John Doe");
        assertThat(result.getCurrency()).isEqualTo("CHF");
    }

    @Test
    @DisplayName("should return account when account exists")
    void should_ReturnAccount_When_AccountExists() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        Account result = accountService.getAccount(accountId);

        assertThat(result).isNotNull();
        assertThat(result.getOwnerName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when account does not exist")
    void should_ThrowException_When_AccountNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(accountId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Account")
            .hasMessageContaining(accountId.toString());
    }
}
