package com.finflow.transaction.infrastructure;

import com.finflow.shared.config.KafkaConfig;
import com.finflow.transaction.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox table every 500 ms and publishes pending events to Kafka.
 * Each batch is processed in a single DB transaction so status updates
 * are committed atomically.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Reads up to {@value BATCH_SIZE} PENDING outbox events and publishes each one
     * to the {@code transaction-events} Kafka topic.
     * The event key is the {@code aggregateId} so events for the same entity
     * land on the same partition (ordering guarantee).
     */
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> batch = outboxRepository
            .findByStatusOrderByCreatedAtAsc("PENDING")
            .stream()
            .limit(BATCH_SIZE)
            .toList();

        if (batch.isEmpty()) {
            return;
        }

        int sent = 0;

        for (OutboxEvent event : batch) {
            if (event.getRetryCount() >= MAX_RETRIES) {
                log.warn("Skipping outbox event {} (type={}): retry_count={} exceeds max retries",
                    event.getId(), event.getEventType(), event.getRetryCount());
                continue;
            }

            try {
                kafkaTemplate.send(
                    KafkaConfig.TRANSACTION_EVENTS_TOPIC,
                    event.getAggregateId().toString(),
                    event.getPayload()
                ).get();

                event.markAsSent();
                outboxRepository.save(event);
                sent++;

            } catch (Exception e) {
                log.error("Failed to publish outbox event {} (type={}): {}",
                    event.getId(), event.getEventType(), e.getMessage());

                event.markAsFailed();
                outboxRepository.save(event);

                if (event.getRetryCount() >= MAX_RETRIES) {
                    log.error("Outbox event {} (type={}) exceeded max retries ({}) and will not be retried",
                        event.getId(), event.getEventType(), MAX_RETRIES);
                }
            }
        }

        if (sent > 0) {
            log.info("Outbox publisher sent {} event(s) to topic '{}'",
                sent, KafkaConfig.TRANSACTION_EVENTS_TOPIC);
        }
    }
}
