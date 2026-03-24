package com.finflow.transaction.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
    @NotNull(message = "Source account ID is required")
    UUID sourceAccountId,

    @NotNull(message = "Target account ID is required")
    UUID targetAccountId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Minimum transfer amount is 0.01")
    BigDecimal amount,

    String description,

    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey
) {}
