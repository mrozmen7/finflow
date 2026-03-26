package com.finflow.transaction.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finflow.transaction.application.OutboxService;
import com.finflow.transaction.domain.OutboxEvent;
import com.finflow.transaction.infrastructure.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService")
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        outboxService = new OutboxService(outboxRepository, objectMapper);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Save Outbox Event")
    class SaveOutboxEvent {

        @Test
        @DisplayName("should save outbox event when transaction completed")
        void should_SaveOutboxEvent_When_TransactionCompleted() {
            UUID aggregateId = UUID.randomUUID();
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

            outboxService.saveEvent("Transaction", aggregateId, "TRANSACTION_COMPLETED",
                new SimplePayload("completed"));

            verify(outboxRepository).save(captor.capture());
            OutboxEvent saved = captor.getValue();
            assertThat(saved.getEventType()).isEqualTo("TRANSACTION_COMPLETED");
            assertThat(saved.getAggregateType()).isEqualTo("Transaction");
            assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
            assertThat(saved.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("should save outbox event when transaction failed")
        void should_SaveOutboxEvent_When_TransactionFailed() {
            UUID aggregateId = UUID.randomUUID();
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

            outboxService.saveEvent("Transaction", aggregateId, "TRANSACTION_FAILED",
                new SimplePayload("failed"));

            verify(outboxRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo("TRANSACTION_FAILED");
        }

        @Test
        @DisplayName("should serialize payload to JSON and store in outbox event")
        void should_SerializePayloadToJson() {
            UUID txId = UUID.randomUUID();
            TransactionPayload payload = new TransactionPayload(txId, new BigDecimal("250.00"),
                "CHF", LocalDateTime.of(2026, 3, 26, 10, 0));
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

            outboxService.saveEvent("Transaction", txId, "TRANSACTION_COMPLETED", payload);

            verify(outboxRepository).save(captor.capture());
            String json = captor.getValue().getPayload();
            assertThat(json).contains(txId.toString());
            assertThat(json).contains("250.00");
            assertThat(json).contains("CHF");
        }
    }

    /** Minimal payload used to verify event type and aggregate fields. */
    record SimplePayload(String result) {}

    /** Richer payload used to verify JSON serialisation. */
    record TransactionPayload(UUID transactionId, BigDecimal amount, String currency,
                              LocalDateTime timestamp) {}
}
