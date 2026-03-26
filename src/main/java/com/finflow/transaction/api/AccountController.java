package com.finflow.transaction.api;

import com.finflow.transaction.api.dto.AccountResponse;
import com.finflow.transaction.api.dto.BalanceResponse;
import com.finflow.transaction.api.dto.CreateAccountRequest;
import com.finflow.transaction.api.dto.DepositRequest;
import com.finflow.transaction.api.mapper.AccountMapper;
import com.finflow.transaction.application.AccountService;
import com.finflow.transaction.domain.Account;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Account management: create accounts and manage balances")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Create a new financial account.
     */
    @Operation(summary = "Create account", description = "Opens a new financial account for the given owner and currency")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body (validation error)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
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
    @Operation(summary = "Get account", description = "Returns account details for the given account ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {

        log.debug("GET /accounts/{}", id);

        Account account = accountService.getAccount(id);

        return ResponseEntity.ok(AccountMapper.toResponse(account));
    }

    /**
     * Deposit funds into an account.
     */
    @Operation(summary = "Deposit funds", description = "Credits the specified amount to the account balance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deposit successful"),
        @ApiResponse(responseCode = "400", description = "Invalid amount or currency mismatch"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable UUID id,
            @Valid @RequestBody DepositRequest request) {

        log.info("POST /accounts/{}/deposit - amount: {}", id, request.amount());

        Account account = accountService.deposit(id, request.amount());

        return ResponseEntity.ok(AccountMapper.toResponse(account));
    }

    /**
     * Get account balance.
     */
    @Operation(summary = "Get balance", description = "Returns the current balance for the given account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance retrieved"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID id) {

        log.debug("GET /accounts/{}/balance", id);

        Account account = accountService.getAccount(id);

        return ResponseEntity.ok(AccountMapper.toBalanceResponse(account));
    }
}
