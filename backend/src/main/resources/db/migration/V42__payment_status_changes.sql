CREATE TABLE payment_status_changes (
    id            BIGSERIAL    PRIMARY KEY,
    payment_id    BIGINT       NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    status_from   VARCHAR(32),                  -- NULL on the creation/initial row
    status_to     VARCHAR(32)  NOT NULL,
    actor_user_id BIGINT       REFERENCES users(id) ON DELETE SET NULL,  -- NULL = system/saga/webhook
    reason        VARCHAR(255),
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payment_status_changes_payment_time
    ON payment_status_changes (payment_id, occurred_at DESC);
