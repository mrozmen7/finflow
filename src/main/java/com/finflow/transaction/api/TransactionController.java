package com.finflow.transaction.api;

import com.finflow.transaction.api.dto.CreateTransactionRequest;
import com.finflow.transaction.api.dto.TransactionResponse;
import com.finflow.transaction.api.mapper.TransactionMapper;
import com.finflow.transaction.application.TransactionService;
import com.finflow.transaction.domain.Transaction;
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
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Create a new transfer between accounts.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody CreateTransactionRequest request) {

        log.info("POST /transactions - Transfer: {} -> {}, amount: {}",
                request.sourceAccountId(), request.targetAccountId(), request.amount());

        Transaction transaction = transactionService.transfer(
            request.sourceAccountId(),
            request.targetAccountId(),
            request.amount(),
            request.description()
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
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {

        Transaction transaction = transactionService.getTransaction(id);

        return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
    }

    /**
     * Get all transactions for an account (paginated).
     */
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
