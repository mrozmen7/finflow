CREATE TABLE fraud_cases (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    transaction_id     UUID         NOT NULL,
    source_account_id  UUID         NOT NULL,
    rule_violated      VARCHAR(100) NOT NULL,
    risk_score         INT          NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    description        VARCHAR(500),
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at        TIMESTAMP WITHOUT TIME ZONE,
    resolved_by        VARCHAR(255),

    CONSTRAINT pk_fraud_cases PRIMARY KEY (id),
    CONSTRAINT chk_fraud_cases_status CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT chk_fraud_cases_risk_score CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_fraud_cases_transaction_id    ON fraud_cases(transaction_id);
CREATE INDEX idx_fraud_cases_source_account_id ON fraud_cases(source_account_id);
CREATE INDEX idx_fraud_cases_status            ON fraud_cases(status);

COMMENT ON TABLE fraud_cases IS 'Detected fraud cases linked to transactions';
COMMENT ON COLUMN fraud_cases.rule_violated IS 'Name of the fraud rule that was triggered';
COMMENT ON COLUMN fraud_cases.risk_score IS 'Risk score 0–100 assigned at detection time';

-- ---------------------------------------------------------------------------

CREATE TABLE fraud_rules (
    id                   UUID           NOT NULL DEFAULT gen_random_uuid(),
    name                 VARCHAR(100)   NOT NULL,
    description          VARCHAR(500),
    enabled              BOOLEAN        NOT NULL DEFAULT true,
    threshold_value      DECIMAL(19, 4),
    time_window_minutes  INT,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_fraud_rules PRIMARY KEY (id),
    CONSTRAINT uq_fraud_rules_name UNIQUE (name)
);

COMMENT ON TABLE fraud_rules IS 'Configurable fraud detection rules';
COMMENT ON COLUMN fraud_rules.threshold_value IS 'Numeric threshold used by the rule (e.g. max amount)';
COMMENT ON COLUMN fraud_rules.time_window_minutes IS 'Sliding time window the rule evaluates (e.g. 60 minutes)';
