CREATE TABLE payment_saga_log (
    id                       BIGSERIAL    PRIMARY KEY,
    provider_transaction_id  VARCHAR(128) NOT NULL,
    step_name                VARCHAR(100) NOT NULL,
    outcome                  VARCHAR(20)  NOT NULL,
    error_message            TEXT,
    executed_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_log_transaction ON payment_saga_log(provider_transaction_id);
CREATE INDEX idx_saga_log_outcome     ON payment_saga_log(outcome);
