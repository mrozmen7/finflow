package com.finflow.fraud.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a detected fraud case linked to a financial transaction.
 */
@Entity
@Table(name = "fraud_cases")
public class FraudCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "rule_violated", nullable = false, length = 100)
    private String ruleViolated;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudCaseStatus status;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    protected FraudCase() {
    }

    /**
     * Opens a new fraud case.
     *
     * @param transactionId   ID of the suspicious transaction
     * @param sourceAccountId ID of the account that initiated the transaction
     * @param ruleViolated    name of the fraud rule that was triggered
     * @param riskScore       risk score in the range 0–100
     * @param description     optional human-readable detail
     */
    public FraudCase(UUID transactionId, UUID sourceAccountId,
                     String ruleViolated, int riskScore, String description) {
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.ruleViolated = ruleViolated;
        this.riskScore = riskScore;
        this.description = description;
        this.status = FraudCaseStatus.OPEN;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Marks this case as confirmed fraud and records who resolved it.
     *
     * @param resolvedBy username or identifier of the resolver
     * @throws IllegalStateException if the case is already closed
     */
    public void resolve(String resolvedBy) {
        if (this.status == FraudCaseStatus.RESOLVED || this.status == FraudCaseStatus.DISMISSED) {
            throw new IllegalStateException(
                "Cannot resolve fraud case in status: " + this.status);
        }
        this.status = FraudCaseStatus.RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Dismisses this case as a false positive and records who dismissed it.
     *
     * @param resolvedBy username or identifier of the dismisser
     * @throws IllegalStateException if the case is already closed
     */
    public void dismiss(String resolvedBy) {
        if (this.status == FraudCaseStatus.RESOLVED || this.status == FraudCaseStatus.DISMISSED) {
            throw new IllegalStateException(
                "Cannot dismiss fraud case in status: " + this.status);
        }
        this.status = FraudCaseStatus.DISMISSED;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public String getRuleViolated() { return ruleViolated; }
    public int getRiskScore() { return riskScore; }
    public FraudCaseStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
}
