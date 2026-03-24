package com.finflow.transaction.api;

import com.finflow.transaction.api.dto.AccountResponse;
import com.finflow.transaction.api.dto.BalanceResponse;
import com.finflow.transaction.api.dto.CreateAccountRequest;
import com.finflow.transaction.api.mapper.AccountMapper;
import com.finflow.transaction.application.AccountService;
import com.finflow.transaction.domain.Account;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Create a new financial account.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        log.info("POST /accounts - Creating account for: {}", request.ownerName());

        Account account = accountService.createAccount(
            request.ownerName(), request.currency()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AccountMapper.toResponse(account));
    }

    /**
     * Get account details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {

        log.debug("GET /accounts/{}", id);

        Account account = accountService.getAccount(id);

        return ResponseEntity.ok(AccountMapper.toResponse(account));
    }

    /**
     * Get account balance.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID id) {

        log.debug("GET /accounts/{}/balance", id);

        Account account = accountService.getAccount(id);

        return ResponseEntity.ok(AccountMapper.toBalanceResponse(account));
    }
}
