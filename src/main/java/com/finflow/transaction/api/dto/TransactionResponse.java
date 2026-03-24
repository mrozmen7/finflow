package com.finflow.transaction.api.dto;

import com.finflow.transaction.domain.TransactionStatus;
import com.finflow.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    String currency,
    TransactionStatus status,
    TransactionType type,
    String description,
    String failureReason,
    LocalDateTime createdAt
) {}
