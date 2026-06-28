CREATE TABLE order_status_changes (
    id            BIGSERIAL    PRIMARY KEY,
    order_id      BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status_from   VARCHAR(32),                  -- NULL on the creation/initial row
    status_to     VARCHAR(32)  NOT NULL,
    actor_user_id BIGINT       REFERENCES users(id) ON DELETE SET NULL,  -- NULL = system/saga
    reason        VARCHAR(255),
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_status_changes_order_time ON order_status_changes (order_id, occurred_at DESC);
