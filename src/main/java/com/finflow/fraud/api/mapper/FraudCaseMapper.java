package com.finflow.fraud.api.mapper;

import com.finflow.fraud.api.dto.FraudCaseResponse;
import com.finflow.fraud.domain.FraudCase;

/**
 * Maps {@link FraudCase} domain objects to API response records.
 */
public class FraudCaseMapper {

    private FraudCaseMapper() {
    }

    /**
     * Converts a {@link FraudCase} entity to a {@link FraudCaseResponse}.
     *
     * @param fraudCase the entity to convert
     * @return the corresponding response record
     */
    public static FraudCaseResponse toResponse(FraudCase fraudCase) {
        return new FraudCaseResponse(
            fraudCase.getId(),
            fraudCase.getTransactionId(),
            fraudCase.getSourceAccountId(),
            fraudCase.getRuleViolated(),
            fraudCase.getRiskScore(),
            fraudCase.getStatus(),
            fraudCase.getDescription(),
            fraudCase.getCreatedAt(),
            fraudCase.getResolvedAt(),
            fraudCase.getResolvedBy()
        );
    }
}
