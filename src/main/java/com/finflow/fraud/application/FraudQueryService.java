package com.finflow.fraud.application;

import com.finflow.fraud.domain.FraudCase;
import com.finflow.fraud.domain.FraudCaseStatus;
import com.finflow.fraud.infrastructure.FraudCaseRepository;
import com.finflow.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for querying and managing {@link FraudCase} lifecycle.
 */
@Service
public class FraudQueryService {

    private static final Logger log = LoggerFactory.getLogger(FraudQueryService.class);

    private final FraudCaseRepository fraudCaseRepository;

    /**
     * @param fraudCaseRepository repository for fraud case persistence
     */
    public FraudQueryService(FraudCaseRepository fraudCaseRepository) {
        this.fraudCaseRepository = fraudCaseRepository;
    }

    /**
     * Returns all fraud cases, ordered by persistence defaults.
     *
     * @param pageable pagination and sorting parameters
     * @return page of all fraud cases
     */
    @Transactional(readOnly = true)
    public Page<FraudCase> getAllCases(Pageable pageable) {
        return fraudCaseRepository.findAll(pageable);
    }

    /**
     * Returns a single fraud case by ID.
     *
     * @param id the fraud case identifier
     * @return the matching fraud case
     * @throws ResourceNotFoundException if no case with the given ID exists
     */
    @Transactional(readOnly = true)
    public FraudCase getCaseById(UUID id) {
        return fraudCaseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FraudCase", "id", id));
    }

    /**
     * Returns fraud cases filtered by status.
     *
     * @param status   the status to filter by
     * @param pageable pagination and sorting parameters
     * @return page of matching fraud cases
     */
    @Transactional(readOnly = true)
    public Page<FraudCase> getCasesByStatus(FraudCaseStatus status, Pageable pageable) {
        return fraudCaseRepository.findByStatus(status, pageable);
    }

    /**
     * Returns all fraud cases originating from a given account, newest first.
     *
     * @param accountId the source account identifier
     * @return list of fraud cases for the account
     */
    @Transactional(readOnly = true)
    public List<FraudCase> getCasesByAccountId(UUID accountId) {
        return fraudCaseRepository.findBySourceAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Marks a fraud case as RESOLVED.
     *
     * @param id         the fraud case identifier
     * @param resolvedBy username or identifier of the resolver
     * @return the updated fraud case
     * @throws ResourceNotFoundException if no case with the given ID exists
     * @throws IllegalStateException     if the case is already closed
     */
    @Transactional
    public FraudCase resolveCase(UUID id, String resolvedBy) {
        FraudCase fraudCase = fraudCaseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FraudCase", "id", id));
        fraudCase.resolve(resolvedBy);
        FraudCase saved = fraudCaseRepository.save(fraudCase);
        log.info("Fraud case {} resolved by {}", id, resolvedBy);
        return saved;
    }

    /**
     * Marks a fraud case as DISMISSED (false positive).
     *
     * @param id         the fraud case identifier
     * @param resolvedBy username or identifier of the dismisser
     * @return the updated fraud case
     * @throws ResourceNotFoundException if no case with the given ID exists
     * @throws IllegalStateException     if the case is already closed
     */
    @Transactional
    public FraudCase dismissCase(UUID id, String resolvedBy) {
        FraudCase fraudCase = fraudCaseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FraudCase", "id", id));
        fraudCase.dismiss(resolvedBy);
        FraudCase saved = fraudCaseRepository.save(fraudCase);
        log.info("Fraud case {} dismissed by {}", id, resolvedBy);
        return saved;
    }
}
