package com.finflow.fraud.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finflow.fraud.application.FraudAnalysisService;
import com.finflow.fraud.domain.TransactionEventPayload;
import com.finflow.fraud.infrastructure.TransactionEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionEventConsumer")
class TransactionEventConsumerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FraudAnalysisService fraudAnalysisService;

    private TransactionEventConsumer consumer;
    private ObjectMapper objectMapper;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        consumer = new TransactionEventConsumer(objectMapper, redisTemplate, fraudAnalysisService);
        transactionId = UUID.randomUUID();
    }

    private String buildEventJson(UUID txId) throws Exception {
        TransactionEventPayload event = new TransactionEventPayload(
            txId, UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("500"), "CHF", "COMPLETED",
            "test transfer", LocalDateTime.of(2026, 3, 26, 10, 0));
        return objectMapper.writeValueAsString(event);
    }

    @Nested
    @DisplayName("Event Processing")
    class EventProcessing {

        @Test
        @DisplayName("should call fraud analysis and set idempotency key on first-time event")
        void should_ProcessEvent_When_FirstTime() throws Exception {
            String json = buildEventJson(transactionId);
            when(redisTemplate.hasKey("fraud-processed:" + transactionId)).thenReturn(false);

            consumer.consume(json);

            verify(fraudAnalysisService).analyze(any(TransactionEventPayload.class));
            verify(valueOperations).set(eq("fraud-processed:" + transactionId),
                eq("1"), any(Duration.class));
        }

        @Test
        @DisplayName("should skip processing and not call fraud analysis when event was already processed")
        void should_SkipEvent_When_AlreadyProcessed() throws Exception {
            String json = buildEventJson(transactionId);
            when(redisTemplate.hasKey("fraud-processed:" + transactionId)).thenReturn(true);

            consumer.consume(json);

            verify(fraudAnalysisService, never()).analyze(any());
            verify(valueOperations, never()).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("should pass the correctly deserialized event to fraud analysis")
        void should_CallFraudAnalysis_When_ValidEvent() throws Exception {
            TransactionEventPayload expected = new TransactionEventPayload(
                transactionId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("60000"), "CHF", "COMPLETED",
                "large transfer", LocalDateTime.of(2026, 3, 26, 10, 0));
            String json = objectMapper.writeValueAsString(expected);
            when(redisTemplate.hasKey("fraud-processed:" + transactionId)).thenReturn(false);

            consumer.consume(json);

            verify(fraudAnalysisService).analyze(argThat(event ->
                event.transactionId().equals(transactionId)
                && event.amount().compareTo(new BigDecimal("60000")) == 0
                && "CHF".equals(event.currency())
                && "COMPLETED".equals(event.status())
            ));
        }
    }
}
