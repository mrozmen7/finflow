package com.finflow.transaction.unit;

import com.finflow.shared.exception.ResourceNotFoundException;
import com.finflow.shared.util.IdempotencyService;
import com.finflow.shared.util.RateLimiterService;
import com.finflow.transaction.application.OutboxService;
import com.finflow.transaction.application.TransactionService;
import com.finflow.transaction.domain.Account;
import com.finflow.transaction.domain.AccountStatus;
import com.finflow.transaction.domain.Transaction;
import com.finflow.transaction.domain.TransactionStatus;
import com.finflow.transaction.infrastructure.AccountRepository;
import com.finflow.transaction.infrastructure.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TransactionService transactionService;

    private static final String IDEMPOTENCY_KEY = "test-key-" + UUID.randomUUID();

    private UUID sourceId;
    private UUID targetId;
    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        sourceId = UUID.randomUUID();
        targetId = UUID.randomUUID();

        source = new Account("Source Owner", "CHF");
        source.deposit(new BigDecimal("1000"));

        target = new Account("Target Owner", "CHF");

        lenient().when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(i -> i.getArgument(0));
        lenient().when(accountRepository.save(any(Account.class)))
            .thenAnswer(i -> i.getArgument(0));
        lenient().when(idempotencyService.get(any())).thenReturn(Optional.empty());
        lenient().when(rateLimiterService.isAllowed(any())).thenReturn(true);
    }

    @Nested
    @DisplayName("Successful Transfer")
    class SuccessfulTransfer {

        @BeforeEach
        void setUp() {
            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));
        }

        @Test
        @DisplayName("should complete transfer when source has sufficient balance")
        void should_CompleteTransfer_When_SufficientBalance() {
            Transaction result = transactionService.transfer(
                sourceId, targetId, new BigDecimal("500"), "Test transfer", IDEMPOTENCY_KEY, "SYSTEM");

            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        }

        @Test
        @DisplayName("should deduct amount from source account when transfer completes")
        void should_DeductFromSource_When_TransferCompleted() {
            transactionService.transfer(sourceId, targetId, new BigDecimal("500"), null, IDEMPOTENCY_KEY, "SYSTEM");

            assertThat(source.getBalance()).isEqualByComparingTo(new BigDecimal("500"));
        }

        @Test
        @DisplayName("should add amount to target account when transfer completes")
        void should_AddToTarget_When_TransferCompleted() {
            transactionService.transfer(sourceId, targetId, new BigDecimal("500"), null, IDEMPOTENCY_KEY, "SYSTEM");

            assertThat(target.getBalance()).isEqualByComparingTo(new BigDecimal("500"));
        }
    }

    @Nested
    @DisplayName("Failed Transfer")
    class FailedTransfer {

        @BeforeEach
        void setUp() {
            Account lowBalanceSource = new Account("Source Owner", "CHF");
            lowBalanceSource.deposit(new BigDecimal("100"));

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(lowBalanceSource));
            when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));
        }

        @Test
        @DisplayName("should fail transfer when source has insufficient balance")
        void should_FailTransfer_When_InsufficientBalance() {
            Transaction result = transactionService.transfer(
                sourceId, targetId, new BigDecimal("500"), null, IDEMPOTENCY_KEY, "SYSTEM");

            assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
            assertThat(result.getFailureReason()).isNotBlank();
        }

        @Test
        @DisplayName("should not change balances when transfer fails due to insufficient funds")
        void should_NotChangeBalances_When_TransferFails() {
            transactionService.transfer(sourceId, targetId, new BigDecimal("500"), null, IDEMPOTENCY_KEY, "SYSTEM");

            verify(accountRepository, never()).save(any(Account.class));
            assertThat(target.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Idempotency and Currency")
    class IdempotencyAndCurrency {

        @Test
        @DisplayName("should return same result when same idempotency key is reused")
        void should_ReturnSameResult_When_DuplicateIdempotencyKey() {
            UUID existingTxId = UUID.randomUUID();
            Transaction existingTx = new Transaction(
                sourceId, targetId, new BigDecimal("500"), "CHF", "Already processed", "SYSTEM");
            existingTx.complete();

            when(idempotencyService.get(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingTxId));
            when(transactionRepository.findById(existingTxId)).thenReturn(Optional.of(existingTx));

            Transaction result = transactionService.transfer(
                sourceId, targetId, new BigDecimal("500"), null, IDEMPOTENCY_KEY, "SYSTEM");

            assertThat(result).isSameAs(existingTx);
            verify(accountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when source and target currencies differ")
        void should_ThrowException_When_CurrencyMismatch() {
            Account usdTarget = new Account("USD Owner", "USD");

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(accountRepository.findById(targetId)).thenReturn(Optional.of(usdTarget));

            assertThatThrownBy(() ->
                transactionService.transfer(sourceId, targetId, new BigDecimal("100"), null, IDEMPOTENCY_KEY, "SYSTEM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
        }
    }

    @Nested
    @DisplayName("Reverse Transaction")
    class ReverseTransaction {

        @Test
        @DisplayName("should reverse transaction and return amount to source when status is FAILED")
        void should_ReverseTransaction_When_StatusFailed() {
            UUID txId = UUID.randomUUID();
            Transaction failedTx = new Transaction(
                sourceId, targetId, new BigDecimal("200"), "CHF", "To reverse", "SYSTEM");
            failedTx.fail("Insufficient balance");

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(failedTx));
            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

            Transaction result = transactionService.reverseTransaction(txId);

            assertThat(result.getStatus()).isEqualTo(TransactionStatus.REVERSED);
            assertThat(source.getBalance()).isEqualByComparingTo(new BigDecimal("1200"));
            verify(accountRepository).save(source);
        }

        @Test
        @DisplayName("should throw IllegalStateException when transaction status is not FAILED")
        void should_ThrowException_When_StatusNotFailed() {
            UUID txId = UUID.randomUUID();
            Transaction completedTx = new Transaction(
                sourceId, targetId, new BigDecimal("200"), "CHF", "Completed", "SYSTEM");
            completedTx.complete();

            when(transactionRepository.findById(txId)).thenReturn(Optional.of(completedTx));

            assertThatThrownBy(() -> transactionService.reverseTransaction(txId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reverse transaction with status");
        }
    }

    @Nested
    @DisplayName("Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("should throw IllegalArgumentException when transferring to same account")
        void should_ThrowException_When_SameAccount() {
            assertThatThrownBy(() ->
                transactionService.transfer(sourceId, sourceId, new BigDecimal("100"), null, IDEMPOTENCY_KEY, "SYSTEM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same account");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when source account does not exist")
        void should_ThrowException_When_SourceNotFound() {
            when(accountRepository.findById(sourceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                transactionService.transfer(sourceId, targetId, new BigDecimal("100"), null, IDEMPOTENCY_KEY, "SYSTEM"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(sourceId.toString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when target account does not exist")
        void should_ThrowException_When_TargetNotFound() {
            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(accountRepository.findById(targetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                transactionService.transfer(sourceId, targetId, new BigDecimal("100"), null, IDEMPOTENCY_KEY, "SYSTEM"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(targetId.toString());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount exceeds maximum limit")
        void should_ThrowException_When_AmountExceedsLimit() {
            assertThatThrownBy(() ->
                transactionService.transfer(sourceId, targetId, new BigDecimal("1000001"), null, IDEMPOTENCY_KEY, "SYSTEM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum limit");
        }

        @Test
        @DisplayName("should throw IllegalStateException when target account is frozen")
        void should_ThrowException_When_TargetAccountFrozen() {
            Account frozenTarget = new Account("Frozen Target", "CHF") {
                @Override
                public AccountStatus getStatus() { return AccountStatus.FROZEN; }
            };

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(accountRepository.findById(targetId)).thenReturn(Optional.of(frozenTarget));

            assertThatThrownBy(() ->
                transactionService.transfer(sourceId, targetId, new BigDecimal("100"), null, IDEMPOTENCY_KEY, "SYSTEM"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("should throw IllegalStateException when source account is not active")
        void should_ThrowException_When_AccountNotActive() {
            Account frozenAccount = new Account("Frozen Owner", "CHF");
            frozenAccount.deposit(new BigDecimal("1000"));
            // Simulate frozen status via reflection workaround — use a subclass trick
            Account frozenSource = new Account("Frozen Owner", "CHF") {
                @Override
                public AccountStatus getStatus() { return AccountStatus.FROZEN; }
            };
            frozenSource.deposit(new BigDecimal("1000"));

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(frozenSource));
            when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));

            assertThatThrownBy(() ->
                transactionService.transfer(sourceId, targetId, new BigDecimal("100"), null, IDEMPOTENCY_KEY, "SYSTEM"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
        }
    }
}
