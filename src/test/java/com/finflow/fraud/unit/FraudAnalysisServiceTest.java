package com.finflow.fraud.unit;

import com.finflow.fraud.application.FraudAnalysisService;
import com.finflow.fraud.domain.FraudCase;
import com.finflow.fraud.domain.FraudCaseStatus;
import com.finflow.fraud.domain.FraudRule;
import com.finflow.fraud.domain.TransactionEventPayload;
import com.finflow.fraud.infrastructure.FraudCaseRepository;
import com.finflow.fraud.infrastructure.FraudRuleRepository;
import com.finflow.transaction.application.OutboxService;
import com.finflow.transaction.application.TransactionMetrics;
import com.finflow.transaction.domain.Transaction;
import com.finflow.transaction.domain.TransactionStatus;
import com.finflow.transaction.infrastructure.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudAnalysisService")
class FraudAnalysisServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private TransactionMetrics transactionMetrics;

    @InjectMocks
    private FraudAnalysisService fraudAnalysisService;

    private UUID transactionId;
    private UUID sourceAccountId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        sourceAccountId = UUID.randomUUID();

        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(
            new FraudRule("HIGH_AMOUNT", "Flags high-value transactions",
                new BigDecimal("50000"), null)
        ));
        lenient().when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(i -> i.getArgument(0));
    }

    private TransactionEventPayload buildEvent(BigDecimal amount, String status) {
        return new TransactionEventPayload(transactionId, sourceAccountId, UUID.randomUUID(),
            amount, "CHF", status, "test transfer", LocalDateTime.now());
    }

    @Nested
    @DisplayName("High Amount Rule")
    class HighAmountRule {

        @Test
        @DisplayName("should create fraud case when amount exceeds 50000")
        void should_CreateFraudCase_When_HighAmount() {
            TransactionEventPayload event = buildEvent(new BigDecimal("60000"), "COMPLETED");
            Transaction completedTx = new Transaction(sourceAccountId, UUID.randomUUID(),
                new BigDecimal("60000"), "CHF", "test", "SYSTEM");
            completedTx.complete();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(completedTx));

            fraudAnalysisService.analyze(event);

            ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
            verify(fraudCaseRepository).save(captor.capture());
            FraudCase saved = captor.getValue();
            assertThat(saved.getRuleViolated()).isEqualTo("HIGH_AMOUNT");
            assertThat(saved.getRiskScore()).isEqualTo(80);
            assertThat(saved.getStatus()).isEqualTo(FraudCaseStatus.OPEN);
            assertThat(saved.getTransactionId()).isEqualTo(transactionId);
        }

        @Test
        @DisplayName("should not create fraud case when amount is below threshold")
        void should_NotCreateFraudCase_When_NormalAmount() {
            TransactionEventPayload event = buildEvent(new BigDecimal("100"), "COMPLETED");

            fraudAnalysisService.analyze(event);

            verify(fraudCaseRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should flag transaction when fraud is detected")
        void should_FlagTransaction_When_FraudDetected() {
            TransactionEventPayload event = buildEvent(new BigDecimal("75000"), "COMPLETED");
            Transaction completedTx = new Transaction(sourceAccountId, UUID.randomUUID(),
                new BigDecimal("75000"), "CHF", "test", "SYSTEM");
            completedTx.complete();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(completedTx));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

            fraudAnalysisService.analyze(event);

            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.FLAGGED);
        }

        @Test
        @DisplayName("should use HIGH_AMOUNT risk score even when multiple rules are active")
        void should_UseHighestRiskScore_When_MultipleRulesViolated() {
            when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(
                new FraudRule("HIGH_AMOUNT", "High amount rule", new BigDecimal("50000"), null),
                new FraudRule("VELOCITY", "Rapid transfer rule", null, 60)
            ));
            TransactionEventPayload event = buildEvent(new BigDecimal("99000"), "COMPLETED");
            Transaction completedTx = new Transaction(sourceAccountId, UUID.randomUUID(),
                new BigDecimal("99000"), "CHF", "test", "SYSTEM");
            completedTx.complete();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(completedTx));

            fraudAnalysisService.analyze(event);

            ArgumentCaptor<FraudCase> captor = ArgumentCaptor.forClass(FraudCase.class);
            verify(fraudCaseRepository).save(captor.capture());
            assertThat(captor.getValue().getRiskScore()).isEqualTo(80);
        }
    }
}
