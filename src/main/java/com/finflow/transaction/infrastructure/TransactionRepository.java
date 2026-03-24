package com.finflow.transaction.infrastructure;

import com.finflow.transaction.domain.Transaction;
import com.finflow.transaction.domain.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findBySourceAccountIdOrTargetAccountId(
        UUID sourceAccountId, UUID targetAccountId, Pageable pageable);

    List<Transaction> findByStatus(TransactionStatus status);
}
