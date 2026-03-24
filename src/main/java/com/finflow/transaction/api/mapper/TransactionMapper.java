package com.finflow.transaction.api.mapper;

import com.finflow.transaction.api.dto.TransactionResponse;
import com.finflow.transaction.domain.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getSourceAccountId(),
            transaction.getTargetAccountId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getStatus(),
            transaction.getType(),
            transaction.getDescription(),
            transaction.getFailureReason(),
            transaction.getCreatedAt()
        );
    }
}
