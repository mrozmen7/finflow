package com.finflow.transaction.api;

import com.finflow.transaction.api.dto.CreateTransactionRequest;
import com.finflow.transaction.api.dto.TransactionResponse;
import com.finflow.transaction.api.mapper.TransactionMapper;
import com.finflow.transaction.application.TransactionService;
import com.finflow.transaction.domain.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions", description = "Payment transfers: initiate, query, and reverse transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Create a new transfer between accounts.
     */
    @Operation(summary = "Create transfer", description = "Initiates a fund transfer between two accounts. Returns 201 on success, 409 if the idempotency key was already used.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body (validation error)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Source or target account not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate idempotency key — transfer already processed")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody CreateTransactionRequest request) {

        log.info("POST /transactions - Transfer: {} -> {}, amount: {}",
                request.sourceAccountId(), request.targetAccountId(), request.amount());

        Transaction transaction = transactionService.transfer(
            request.sourceAccountId(),
            request.targetAccountId(),
            request.amount(),
            request.description(),
            request.idempotencyKey(),
            "SYSTEM" // TODO: replace with authenticated user when JWT is implemented
        );

        HttpStatus status = transaction.getStatus().name().equals("COMPLETED")
            ? HttpStatus.CREATED
            : HttpStatus.CONFLICT;

        return ResponseEntity.status(status)
            .body(TransactionMapper.toResponse(transaction));
    }

    /**
     * Get transaction details by ID.
     */
    @Operation(summary = "Get transaction", description = "Returns details for a single transaction by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {

        Transaction transaction = transactionService.getTransaction(id);

        return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
    }

    /**
     * Reverse a failed transaction, returning the amount to the source account.
     */
    @Operation(summary = "Reverse transaction", description = "Reverses a FAILED transaction, refunding the amount to the source account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction reversed successfully"),
        @ApiResponse(responseCode = "400", description = "Transaction is not in a reversible state"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PostMapping("/{id}/reverse")
    public ResponseEntity<TransactionResponse> reverseTransaction(@PathVariable UUID id) {

        log.info("POST /transactions/{}/reverse", id);

        Transaction transaction = transactionService.reverseTransaction(id);

        return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
    }

    /**
     * Get all transactions for an account (paginated).
     */
    @Operation(summary = "List account transactions", description = "Returns a paginated list of all transactions for the given account (default page size: 20)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<TransactionResponse> transactions = transactionService
            .getAccountTransactions(accountId, pageable)
            .map(TransactionMapper::toResponse);

        return ResponseEntity.ok(transactions);
    }
}
