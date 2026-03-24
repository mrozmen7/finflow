package com.finflow.transaction.api.dto;

import com.finflow.transaction.domain.AccountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String ownerName,
    String currency,
    BigDecimal balance,
    AccountStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
