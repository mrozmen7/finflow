package com.finflow.transaction.application;

import com.finflow.shared.exception.RateLimitExceededException;
import com.finflow.shared.exception.ResourceNotFoundException;
import com.finflow.shared.util.IdempotencyService;
import com.finflow.shared.util.RateLimiterService;
import com.finflow.transaction.domain.Account;
import com.finflow.transaction.domain.AccountStatus;
import com.finflow.transaction.domain.Transaction;
import com.finflow.transaction.infrastructure.AccountRepository;
import com.finflow.transaction.infrastructure.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("1000000");

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final IdempotencyService idempotencyService;
    private final RateLimiterService rateLimiterService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              IdempotencyService idempotencyService,
                              RateLimiterService rateLimiterService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.idempotencyService = idempotencyService;
        this.rateLimiterService = rateLimiterService;
    }

    @Transactional
    public Transaction transfer(UUID sourceAccountId, UUID targetAccountId,
                                BigDecimal amount, String description, String idempotencyKey) {

        // Rate limit check — max 10 transfers per account per minute
        if (!rateLimiterService.isAllowed(sourceAccountId)) {
            throw new RateLimitExceededException(
                "Transfer rate limit exceeded for account: " + sourceAccountId
                + ". Please retry after one minute.");
        }

        // Idempotency check — return existing transaction if key already processed
        Optional<UUID> existing = idempotencyService.get(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key: {}, returning existing transaction: {}",
                     idempotencyKey, existing.get());
            return transactionRepository.findById(existing.get())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", existing.get()));
        }

        log.info("Transfer initiated: {} -> {}, amount: {}",
                 sourceAccountId, targetAccountId, amount);

        // Validation
        validateTransferRequest(sourceAccountId, targetAccountId, amount);

        // Fetch accounts
        Account source = accountRepository.findById(sourceAccountId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Account", "id", sourceAccountId));

        Account target = accountRepository.findById(targetAccountId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Account", "id", targetAccountId));

        // Validate account status
        validateAccountStatus(source, "Source");
        validateAccountStatus(target, "Target");

        // Validate currency match
        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new IllegalArgumentException(
                "Currency mismatch: source=" + source.getCurrency()
                + ", target=" + target.getCurrency());
        }

        // Create transaction record
        Transaction transaction = new Transaction(
            sourceAccountId, targetAccountId,
            amount, source.getCurrency(), description
        );

        try {
            // Execute transfer
            source.withdraw(amount);
            target.deposit(amount);

            // Save updated accounts
            accountRepository.save(source);
            accountRepository.save(target);

            // Mark transaction as completed
            transaction.complete();

            log.info("Transfer completed: {} -> {}, amount: {}",
                     sourceAccountId, targetAccountId, amount);

        } catch (IllegalStateException e) {
            // Insufficient balance or other business rule violation
            transaction.fail(e.getMessage());

            log.warn("Transfer failed: {} -> {}, reason: {}",
                     sourceAccountId, targetAccountId, e.getMessage());
        }

        Transaction saved = transactionRepository.save(transaction);
        idempotencyService.store(idempotencyKey, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Transaction", "id", transactionId));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getAccountTransactions(UUID accountId, Pageable pageable) {
        return transactionRepository.findBySourceAccountIdOrTargetAccountId(
            accountId, accountId, pageable);
    }

    private void validateTransferRequest(UUID sourceId, UUID targetId, BigDecimal amount) {
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                "Transfer amount exceeds maximum limit of " + MAX_TRANSFER_AMOUNT);
        }
    }

    private void validateAccountStatus(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                label + " account is not active: " + account.getId());
        }
    }
}
