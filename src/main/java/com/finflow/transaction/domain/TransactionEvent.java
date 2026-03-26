package com.finflow.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable snapshot of a transaction at the moment an outbox event is written.
 * Serialised to JSON and stored in {@code outbox_events.payload}.
 */
public record TransactionEvent(
    UUID transactionId,
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    String currency,
    String status,
    String description,
    LocalDateTime timestamp
) {

    /**
     * Builds a {@code TransactionEvent} from a persisted {@link Transaction}.
     *
     * @param transaction the saved transaction whose state should be captured
     * @return event snapshot with the current timestamp
     */
    public static TransactionEvent from(Transaction transaction) {
        return new TransactionEvent(
            transaction.getId(),
            transaction.getSourceAccountId(),
            transaction.getTargetAccountId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getStatus().name(),
            transaction.getDescription(),
            LocalDateTime.now()
        );
    }
}
