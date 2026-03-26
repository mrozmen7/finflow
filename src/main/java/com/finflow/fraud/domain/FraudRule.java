package com.finflow.fraud.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A configurable fraud detection rule. Rules are evaluated by the
 * fraud engine when a {@code TRANSACTION_COMPLETED} event is received.
 */
@Entity
@Table(name = "fraud_rules")
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "threshold_value", precision = 19, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FraudRule() {
    }

    /**
     * Creates a new fraud rule in enabled state.
     *
     * @param name               unique rule identifier
     * @param description        human-readable explanation
     * @param thresholdValue     numeric threshold used by the rule (may be {@code null})
     * @param timeWindowMinutes  sliding evaluation window in minutes (may be {@code null})
     */
    public FraudRule(String name, String description,
                     BigDecimal thresholdValue, Integer timeWindowMinutes) {
        this.name = name;
        this.description = description;
        this.thresholdValue = thresholdValue;
        this.timeWindowMinutes = timeWindowMinutes;
        this.enabled = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public Integer getTimeWindowMinutes() { return timeWindowMinutes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
