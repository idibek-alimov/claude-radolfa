-- Idempotency key tracking for ERP sync endpoints (orders, loyalty).
-- Prevents duplicate processing on network retries.

CREATE TABLE erp_sync_idempotency (
    id              BIGSERIAL       PRIMARY KEY,
    idempotency_key VARCHAR(128)    NOT NULL,
    event_type      VARCHAR(32)     NOT NULL,   -- 'ORDER' or 'LOYALTY'
    response_status INT             NOT NULL,   -- HTTP status returned (200, 422, 500, etc.)
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT now(),

    CONSTRAINT uq_idempotency_key_event UNIQUE (idempotency_key, event_type)
);
