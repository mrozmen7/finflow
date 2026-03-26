package com.finflow.fraud.api;

import com.finflow.fraud.api.dto.FraudCaseResponse;
import com.finflow.fraud.api.mapper.FraudCaseMapper;
import com.finflow.fraud.application.FraudQueryService;
import com.finflow.fraud.domain.FraudCaseStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Fraud Cases", description = "Fraud case management: query, resolve, and dismiss detected fraud")
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
    @Operation(summary = "List all fraud cases", description = "Returns a paginated list of all fraud cases regardless of status (default page size: 20)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fraud cases retrieved"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
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
    @Operation(summary = "Get fraud case", description = "Returns details of a single fraud case by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fraud case found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Fraud case not found")
    })
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
    @Operation(summary = "List fraud cases by status", description = "Returns a paginated list of fraud cases matching the given status (OPEN, RESOLVED, DISMISSED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fraud cases retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
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
    @Operation(summary = "List fraud cases by account", description = "Returns all fraud cases associated with the given account, ordered newest first")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fraud cases retrieved"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
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
    @Operation(summary = "Resolve fraud case", description = "Marks an OPEN fraud case as RESOLVED after manual review confirms fraud")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fraud case resolved"),
        @ApiResponse(responseCode = "400", description = "Case is not in OPEN status"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Fraud case not found")
    })
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
    @Operation(summary = "Dismiss fraud case", description = "Dismisses an OPEN fraud case as a false positive")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fraud case dismissed"),
        @ApiResponse(responseCode = "400", description = "Case is not in OPEN status"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Fraud case not found")
    })
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<FraudCaseResponse> dismissCase(
            @PathVariable UUID id,
            @RequestParam String resolvedBy) {

        log.info("POST /fraud/cases/{}/dismiss by {}", id, resolvedBy);
        return ResponseEntity.ok(FraudCaseMapper.toResponse(fraudQueryService.dismissCase(id, resolvedBy)));
    }
}
