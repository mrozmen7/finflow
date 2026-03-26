package com.finflow.fraud.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.fraud.application.FraudAnalysisService;
import com.finflow.fraud.domain.TransactionEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Kafka consumer for the {@code transaction-events} topic.
 * Delegates fraud analysis to {@link FraudAnalysisService} and uses Redis to
 * guarantee exactly-once processing per transaction (idempotency guard).
 */
@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "fraud-processed:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(48);

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final FraudAnalysisService fraudAnalysisService;

    /**
     * @param objectMapper         Jackson mapper for deserialising the event payload
     * @param redisTemplate        Redis client used for idempotency checks
     * @param fraudAnalysisService service that evaluates fraud rules
     */
    public TransactionEventConsumer(ObjectMapper objectMapper,
                                    RedisTemplate<String, String> redisTemplate,
                                    FraudAnalysisService fraudAnalysisService) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.fraudAnalysisService = fraudAnalysisService;
    }

    /**
     * Processes a single message from the {@code transaction-events} topic.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Deserialise JSON payload into {@link TransactionEventPayload}.</li>
     *   <li>Check Redis for the idempotency key; skip if already processed.</li>
     *   <li>Invoke {@link FraudAnalysisService#analyze(TransactionEventPayload)}.</li>
     *   <li>Write the idempotency key to Redis with a 48-hour TTL.</li>
     * </ol>
     *
     * <p>Exceptions propagate to Spring Kafka's {@code DefaultErrorHandler}, which retries
     * up to 3 times with a 1-second fixed delay before routing the message to
     * {@code transaction-events-dlq}.
     *
     * @param message raw JSON string received from Kafka
     * @throws Exception if deserialisation or processing fails; handled by the error handler
     */
    @KafkaListener(topics = "transaction-events", groupId = "finflow-fraud-group")
    public void consume(String message) throws Exception {
        log.debug("Received Kafka event on transaction-events: {}", message);

        TransactionEventPayload event = objectMapper.readValue(message, TransactionEventPayload.class);
        String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + event.transactionId();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey))) {
            log.info("Event already processed for transaction: {}, skipping", event.transactionId());
            return;
        }

        fraudAnalysisService.analyze(event);

        redisTemplate.opsForValue().set(idempotencyKey, "1", IDEMPOTENCY_TTL);
        log.debug("Idempotency key set for transaction: {}", event.transactionId());
    }
}
