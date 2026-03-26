package com.finflow.fraud.api.dto;

import com.finflow.fraud.domain.FraudCaseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API response record for a {@link com.finflow.fraud.domain.FraudCase}.
 */
public record FraudCaseResponse(
    UUID id,
    UUID transactionId,
    UUID sourceAccountId,
    String ruleViolated,
    int riskScore,
    FraudCaseStatus status,
    String description,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt,
    String resolvedBy
) {
}
