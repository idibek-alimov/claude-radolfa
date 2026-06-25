CREATE TABLE discount_changes (
    id            BIGSERIAL     PRIMARY KEY,
    discount_id   BIGINT        REFERENCES discounts(id) ON DELETE SET NULL,
    change_type   VARCHAR(16)   NOT NULL,
    old_value     JSONB,
    new_value     JSONB         NOT NULL,
    actor_user_id BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    occurred_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_discount_changes_discount_time ON discount_changes (discount_id, occurred_at DESC);
