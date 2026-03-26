package com.finflow.transaction.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Micrometer metrics for the transaction module.
 * Exposes counters and timers consumed by Prometheus scraping at
 * {@code /api/v1/actuator/prometheus}.
 */
@Component
public class TransactionMetrics {

    private final Counter completedCounter;
    private final Counter failedCounter;
    private final Counter fraudDetectedCounter;
    private final Timer transferTimer;

    /**
     * @param registry the Micrometer meter registry (auto-configured by Spring Boot)
     */
    public TransactionMetrics(MeterRegistry registry) {
        this.completedCounter = Counter.builder("finflow.transactions.total")
            .description("Total number of processed transactions")
            .tag("status", "COMPLETED")
            .register(registry);
        this.failedCounter = Counter.builder("finflow.transactions.total")
            .description("Total number of processed transactions")
            .tag("status", "FAILED")
            .register(registry);
        this.fraudDetectedCounter = Counter.builder("finflow.transactions.fraud.detected")
            .description("Total number of fraud cases detected")
            .register(registry);
        this.transferTimer = Timer.builder("finflow.transactions.transfer.duration")
            .description("End-to-end transfer processing duration")
            .register(registry);
    }

    /**
     * Increments the transaction counter for the given outcome.
     *
     * @param status {@code "COMPLETED"} or {@code "FAILED"}
     */
    public void recordTransfer(String status) {
        if ("COMPLETED".equals(status)) {
            completedCounter.increment();
        } else {
            failedCounter.increment();
        }
    }

    /**
     * Increments the fraud-detected counter.
     */
    public void recordFraudDetected() {
        fraudDetectedCounter.increment();
    }

    /**
     * Records the transfer processing duration.
     *
     * @param milliseconds elapsed time in milliseconds
     */
    public void recordTransferDuration(long milliseconds) {
        transferTimer.record(milliseconds, TimeUnit.MILLISECONDS);
    }
}
