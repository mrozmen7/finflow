package com.finflow.transaction.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 500)
    private String description;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Transaction() {
    }

    public Transaction(UUID sourceAccountId, UUID targetAccountId,
                       BigDecimal amount, String currency, String description) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = TransactionStatus.PENDING;
        this.type = TransactionType.TRANSFER;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot complete transaction in status: " + this.status);
        }
        this.status = TransactionStatus.COMPLETED;
    }

    public void fail(String reason) {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot fail transaction in status: " + this.status);
        }
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }

    public void flag() {
        if (this.status != TransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                "Cannot flag transaction in status: " + this.status);
        }
        this.status = TransactionStatus.FLAGGED;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public String getFailureReason() { return failureReason; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
