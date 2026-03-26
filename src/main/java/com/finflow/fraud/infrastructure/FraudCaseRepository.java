package com.finflow.fraud.infrastructure;

import com.finflow.fraud.domain.FraudCase;
import com.finflow.fraud.domain.FraudCaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link FraudCase} entities.
 */
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {

    /**
     * Returns the fraud case associated with a specific transaction, if any.
     *
     * @param transactionId the transaction to look up
     * @return the matching fraud case, or empty if none exists
     */
    Optional<FraudCase> findByTransactionId(UUID transactionId);

    /**
     * Returns all fraud cases originating from a given source account,
     * ordered from newest to oldest.
     *
     * @param sourceAccountId the account to look up
     * @return list of fraud cases, may be empty
     */
    List<FraudCase> findBySourceAccountIdOrderByCreatedAtDesc(UUID sourceAccountId);

    /**
     * Returns all fraud cases with the given status.
     *
     * @param status the status to filter by
     * @return list of matching fraud cases
     */
    List<FraudCase> findByStatus(FraudCaseStatus status);

    /**
     * Returns a page of fraud cases with the given status.
     *
     * @param status   the status to filter by
     * @param pageable pagination and sorting parameters
     * @return page of matching fraud cases
     */
    Page<FraudCase> findByStatus(FraudCaseStatus status, Pageable pageable);
}
