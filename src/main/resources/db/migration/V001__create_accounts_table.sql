CREATE TABLE accounts
(
    id         UUID                        NOT NULL DEFAULT gen_random_uuid(),
    owner_name VARCHAR(255)                NOT NULL,
    currency   VARCHAR(3)                  NOT NULL DEFAULT 'CHF',
    balance    NUMERIC(19, 4)              NOT NULL DEFAULT 0,
    status     VARCHAR(20)                 NOT NULL DEFAULT 'ACTIVE',
    version    BIGINT                      NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);
