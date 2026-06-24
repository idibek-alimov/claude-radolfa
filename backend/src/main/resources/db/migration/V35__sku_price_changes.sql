CREATE TABLE sku_price_changes (
    id            BIGSERIAL     PRIMARY KEY,
    sku_id        BIGINT        NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    sku_code      VARCHAR(128)  NOT NULL,
    old_price     NUMERIC(12,2),
    new_price     NUMERIC(12,2) NOT NULL,
    actor_user_id BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    source        VARCHAR(32),
    occurred_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sku_price_changes_sku_time ON sku_price_changes (sku_id, occurred_at DESC);
