-- ================================================================
-- V3__orders.sql
--
-- Orders and line items. References product_variants as the
-- purchasable unit.
-- ================================================================

CREATE TABLE orders (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    erp_order_id    VARCHAR(64)     UNIQUE,
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING' REFERENCES order_statuses(name),
    total_amount    NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id       ON orders (user_id);
CREATE INDEX idx_orders_active        ON orders (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_erp_order_id  ON orders (erp_order_id) WHERE erp_order_id IS NOT NULL;

CREATE TABLE order_items (
    id                BIGSERIAL       PRIMARY KEY,
    order_id          BIGINT          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id        BIGINT          REFERENCES product_variants(id) ON DELETE SET NULL,
    erp_item_code     VARCHAR(128),
    product_name      VARCHAR(255),
    quantity          INTEGER         NOT NULL,
    price_at_purchase NUMERIC(12, 2)  NOT NULL
);

CREATE INDEX idx_order_items_order_id   ON order_items (order_id);
CREATE INDEX idx_order_items_variant_id ON order_items (variant_id);
