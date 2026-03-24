package com.finflow.transaction.application;

import com.finflow.shared.exception.ResourceNotFoundException;
import com.finflow.transaction.domain.Account;
import com.finflow.transaction.infrastructure.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account createAccount(String ownerName, String currency) {
        log.info("Creating account for owner: {}, currency: {}", ownerName, currency);

        Account account = new Account(ownerName, currency);
        Account saved = accountRepository.save(account);

        log.info("Account created successfully: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Account deposit(UUID accountId, BigDecimal amount) {
        log.info("Deposit initiated: accountId={}, amount={}", accountId, amount);

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        if (account.getStatus() != com.finflow.transaction.domain.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active: " + accountId);
        }

        account.deposit(amount);
        Account saved = accountRepository.save(account);

        log.info("Deposit completed: accountId={}, newBalance={}", accountId, saved.getBalance());
        return saved;
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        log.debug("Fetching account: {}", accountId);

        return accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
    }
}
