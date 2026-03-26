package com.finflow.fraud.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable payload published to the {@code fraud-alerts} Kafka topic
 * whenever a {@link FraudCase} is created.
 */
public record FraudAlertEvent(
    UUID fraudCaseId,
    UUID transactionId,
    String ruleViolated,
    int riskScore,
    LocalDateTime timestamp
) {
}
