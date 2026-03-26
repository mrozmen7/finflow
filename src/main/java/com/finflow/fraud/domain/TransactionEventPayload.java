package com.finflow.fraud.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable representation of a transaction event received from the
 * {@code transaction-events} Kafka topic. Mirrors the structure of the
 * producer's {@code TransactionEvent} without creating a cross-module dependency.
 */
public record TransactionEventPayload(
    UUID transactionId,
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    String currency,
    String status,
    String description,
    LocalDateTime timestamp
) {
}
