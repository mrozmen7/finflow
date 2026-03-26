package com.finflow.transaction.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.transaction.domain.OutboxEvent;
import com.finflow.transaction.infrastructure.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes domain events to the transactional outbox table.
 * Must be called within the same DB transaction as the domain change so both
 * are committed or rolled back together.
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Serialises {@code payload} to JSON and persists an {@link OutboxEvent} with PENDING status.
     *
     * @param aggregateType domain entity type, e.g. {@code "Transaction"}
     * @param aggregateId   ID of the entity that produced the event
     * @param eventType     event name, e.g. {@code "TRANSACTION_COMPLETED"}
     * @param payload       object to serialise as the event payload
     * @throws IllegalArgumentException if the payload cannot be serialised to JSON
     */
    @Transactional
    public void saveEvent(String aggregateType, UUID aggregateId,
                          String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                "Failed to serialise outbox payload for event " + eventType, e);
        }

        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, json);
        outboxRepository.save(event);

        log.debug("Outbox event saved: aggregateType={}, aggregateId={}, eventType={}",
            aggregateType, aggregateId, eventType);
    }
}
