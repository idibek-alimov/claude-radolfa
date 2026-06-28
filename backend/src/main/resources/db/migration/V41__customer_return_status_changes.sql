CREATE TABLE customer_return_status_changes (
    id            BIGSERIAL    PRIMARY KEY,
    return_id     BIGINT       NOT NULL REFERENCES customer_returns(id) ON DELETE CASCADE,
    status_from   VARCHAR(32),                  -- NULL on the creation/initial row
    status_to     VARCHAR(32)  NOT NULL,
    actor_user_id BIGINT       REFERENCES users(id) ON DELETE SET NULL,  -- NULL = system
    reason        VARCHAR(255),
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_customer_return_status_changes_return_time
    ON customer_return_status_changes (return_id, occurred_at DESC);
