CREATE TABLE payments (
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT NOT NULL REFERENCES orders(id),
    amount                  NUMERIC(12,2) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'TJS',
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider                VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(255),
    provider_redirect_url   TEXT,
    webhook_payload         TEXT,
    completed_at            TIMESTAMPTZ,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_provider_tx ON payments(provider_transaction_id);
