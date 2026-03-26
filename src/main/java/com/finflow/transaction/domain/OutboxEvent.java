package com.finflow.transaction.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional outbox event. Written in the same DB transaction as the domain change,
 * then picked up by the OutboxPoller and published to Kafka.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    protected OutboxEvent() {
    }

    /**
     * Creates a new outbox event in PENDING status.
     *
     * @param aggregateType domain entity type, e.g. {@code "Transaction"}
     * @param aggregateId   ID of the domain entity that triggered the event
     * @param eventType     event name, e.g. {@code "TRANSACTION_COMPLETED"}
     * @param payload       JSON-serialised event payload
     */
    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = STATUS_PENDING;
        this.retryCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Marks the event as successfully published. Sets status to SENT and records the timestamp.
     *
     * @throws IllegalStateException if the event is not in PENDING status
     */
    public void markAsSent() {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException(
                "Cannot mark outbox event as sent in status: " + this.status);
        }
        this.status = STATUS_SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Marks the event as failed and increments the retry counter.
     *
     * @throws IllegalStateException if the event is not in PENDING status
     */
    public void markAsFailed() {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException(
                "Cannot mark outbox event as failed in status: " + this.status);
        }
        this.status = STATUS_FAILED;
        this.retryCount++;
    }

    /** @return {@code true} if this event has not yet been published */
    public boolean isPending() {
        return STATUS_PENDING.equals(this.status);
    }

    // Getters
    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public int getRetryCount() { return retryCount; }
}
