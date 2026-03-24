package com.finflow.transaction.api.mapper;

import com.finflow.transaction.api.dto.AccountResponse;
import com.finflow.transaction.api.dto.BalanceResponse;
import com.finflow.transaction.domain.Account;

public final class AccountMapper {

    private AccountMapper() {
        // Utility class, no instantiation
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getOwnerName(),
            account.getCurrency(),
            account.getBalance(),
            account.getStatus(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }

    public static BalanceResponse toBalanceResponse(Account account) {
        return new BalanceResponse(
            account.getId(),
            account.getBalance(),
            account.getCurrency()
        );
    }
}
