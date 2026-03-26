package com.finflow.fraud.api;

import com.finflow.fraud.api.dto.FraudCaseResponse;
import com.finflow.fraud.api.mapper.FraudCaseMapper;
import com.finflow.fraud.application.FraudQueryService;
import com.finflow.fraud.domain.FraudCaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for fraud case management.
 * Base path: {@code /api/v1/fraud/cases} (context-path is set globally).
 */
@RestController
@RequestMapping("/fraud/cases")
public class FraudController {

    private static final Logger log = LoggerFactory.getLogger(FraudController.class);

    private final FraudQueryService fraudQueryService;

    /**
     * @param fraudQueryService application service for fraud case queries and mutations
     */
    public FraudController(FraudQueryService fraudQueryService) {
        this.fraudQueryService = fraudQueryService;
    }

    /**
     * Returns all fraud cases, paginated.
     *
     * @param pageable pagination and sorting (default size 20)
     * @return page of fraud case responses
     */
    @GetMapping
    public ResponseEntity<Page<FraudCaseResponse>> getAllCases(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /fraud/cases");
        Page<FraudCaseResponse> cases = fraudQueryService.getAllCases(pageable)
            .map(FraudCaseMapper::toResponse);
        return ResponseEntity.ok(cases);
    }

    /**
     * Returns a single fraud case by ID.
     *
     * @param id the fraud case identifier
     * @return the matching fraud case
     */
    @GetMapping("/{id}")
    public ResponseEntity<FraudCaseResponse> getCaseById(@PathVariable UUID id) {

        log.info("GET /fraud/cases/{}", id);
        return ResponseEntity.ok(FraudCaseMapper.toResponse(fraudQueryService.getCaseById(id)));
    }

    /**
     * Returns fraud cases filtered by status, paginated.
     *
     * @param status   the status to filter by
     * @param pageable pagination and sorting (default size 20)
     * @return page of matching fraud case responses
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<FraudCaseResponse>> getCasesByStatus(
            @PathVariable FraudCaseStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /fraud/cases/status/{}", status);
        Page<FraudCaseResponse> cases = fraudQueryService.getCasesByStatus(status, pageable)
            .map(FraudCaseMapper::toResponse);
        return ResponseEntity.ok(cases);
    }

    /**
     * Returns all fraud cases for a given source account, newest first.
     *
     * @param accountId the account identifier
     * @return list of fraud case responses
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<FraudCaseResponse>> getCasesByAccount(
            @PathVariable UUID accountId) {

        log.info("GET /fraud/cases/account/{}", accountId);
        List<FraudCaseResponse> cases = fraudQueryService.getCasesByAccountId(accountId)
            .stream()
            .map(FraudCaseMapper::toResponse)
            .toList();
        return ResponseEntity.ok(cases);
    }

    /**
     * Marks a fraud case as RESOLVED.
     *
     * @param id         the fraud case identifier
     * @param resolvedBy the username of the resolver (request parameter)
     * @return the updated fraud case
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<FraudCaseResponse> resolveCase(
            @PathVariable UUID id,
            @RequestParam String resolvedBy) {

        log.info("POST /fraud/cases/{}/resolve by {}", id, resolvedBy);
        return ResponseEntity.ok(FraudCaseMapper.toResponse(fraudQueryService.resolveCase(id, resolvedBy)));
    }

    /**
     * Dismisses a fraud case as a false positive.
     *
     * @param id         the fraud case identifier
     * @param resolvedBy the username of the dismisser (request parameter)
     * @return the updated fraud case
     */
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<FraudCaseResponse> dismissCase(
            @PathVariable UUID id,
            @RequestParam String resolvedBy) {

        log.info("POST /fraud/cases/{}/dismiss by {}", id, resolvedBy);
        return ResponseEntity.ok(FraudCaseMapper.toResponse(fraudQueryService.dismissCase(id, resolvedBy)));
    }
}
