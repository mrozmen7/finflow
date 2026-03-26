package com.finflow.fraud.application;

import com.finflow.fraud.domain.FraudAlertEvent;
import com.finflow.fraud.domain.FraudCase;
import com.finflow.fraud.domain.FraudRule;
import com.finflow.fraud.domain.TransactionEventPayload;
import com.finflow.fraud.infrastructure.FraudCaseRepository;
import com.finflow.fraud.infrastructure.FraudRuleRepository;
import com.finflow.transaction.application.OutboxService;
import com.finflow.transaction.domain.Transaction;
import com.finflow.transaction.infrastructure.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evaluates incoming transaction events against active fraud rules.
 * When a rule is violated the service:
 * <ol>
 *   <li>Persists a {@link FraudCase}.</li>
 *   <li>Flags the originating {@link Transaction} (COMPLETED → FLAGGED).</li>
 *   <li>Writes a {@code FRAUD_ALERT} outbox event to the {@code fraud-alerts} topic.</li>
 * </ol>
 *
 * <p><strong>Note:</strong> This service references {@link TransactionRepository} and
 * {@link OutboxService} from the transaction module — a temporary cross-module dependency.
 * A dedicated port/adapter should be introduced when the architecture is formalised.
 *
 * <p>The rule engine is simplified (one hardcoded HIGH_AMOUNT rule) and will be
 * replaced with a full evaluation framework in a later iteration.
 */
@Service
public class FraudAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FraudAnalysisService.class);

    /**
     * Temporary threshold for the HIGH_AMOUNT rule.
     * Will be driven by {@link FraudRule#getThresholdValue()} once the full engine is in place.
     */
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000");
    private static final String HIGH_AMOUNT_RULE = "HIGH_AMOUNT";
    private static final int HIGH_AMOUNT_RISK_SCORE = 80;

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxService outboxService;

    /**
     * @param fraudRuleRepository    repository for loading active rules
     * @param fraudCaseRepository   repository for persisting detected fraud cases
     * @param transactionRepository repository for loading and updating the flagged transaction
     * @param outboxService         outbox writer for publishing fraud alert events
     */
    public FraudAnalysisService(FraudRuleRepository fraudRuleRepository,
                                FraudCaseRepository fraudCaseRepository,
                                TransactionRepository transactionRepository,
                                OutboxService outboxService) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudCaseRepository = fraudCaseRepository;
        this.transactionRepository = transactionRepository;
        this.outboxService = outboxService;
    }

    /**
     * Analyses a transaction event against all currently enabled fraud rules.
     * All DB writes (FraudCase, Transaction flag, OutboxEvent) share the same transaction.
     *
     * @param event the transaction event to evaluate
     */
    @Transactional
    public void analyze(TransactionEventPayload event) {
        List<FraudRule> activeRules = fraudRuleRepository.findByEnabledTrue();
        log.debug("Evaluating {} active fraud rules for transaction: {}",
                  activeRules.size(), event.transactionId());

        // Temporary rule: HIGH_AMOUNT — full rule engine to be implemented in Step 32
        if (event.amount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            FraudCase savedCase = persistFraudCase(event);
            flagTransaction(event);
            publishFraudAlert(savedCase, event);

            log.warn("Transaction {} flagged, fraud case {} created, alert published",
                     event.transactionId(), savedCase.getId());
        } else {
            log.info("Transaction {} passed fraud check (amount: {} {})",
                     event.transactionId(), event.amount(), event.currency());
        }
    }

    private FraudCase persistFraudCase(TransactionEventPayload event) {
        FraudCase fraudCase = new FraudCase(
            event.transactionId(),
            event.sourceAccountId(),
            HIGH_AMOUNT_RULE,
            HIGH_AMOUNT_RISK_SCORE,
            "Amount " + event.amount() + " " + event.currency()
                + " exceeds high-amount threshold of " + HIGH_AMOUNT_THRESHOLD
        );
        return fraudCaseRepository.save(fraudCase);
    }

    private void flagTransaction(TransactionEventPayload event) {
        transactionRepository.findById(event.transactionId()).ifPresentOrElse(
            transaction -> {
                if ("COMPLETED".equals(event.status())) {
                    transaction.flag();
                    transactionRepository.save(transaction);
                } else {
                    log.warn("Skipping flag for transaction {} — status is {}, expected COMPLETED",
                             event.transactionId(), event.status());
                }
            },
            () -> log.warn("Transaction {} not found — cannot flag", event.transactionId())
        );
    }

    private void publishFraudAlert(FraudCase savedCase, TransactionEventPayload event) {
        FraudAlertEvent alertEvent = new FraudAlertEvent(
            savedCase.getId(),
            event.transactionId(),
            HIGH_AMOUNT_RULE,
            HIGH_AMOUNT_RISK_SCORE,
            LocalDateTime.now()
        );
        outboxService.saveEvent("FraudCase", savedCase.getId(), "FRAUD_ALERT", alertEvent);
    }
}
