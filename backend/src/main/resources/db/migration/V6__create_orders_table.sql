-- ================================================================
-- V6__create_orders_table.sql  –  Add orders and order items
-- ================================================================

CREATE TABLE orders (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);

CREATE TABLE order_items (
    id              BIGSERIAL    PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      BIGINT       NOT NULL REFERENCES products(id),
    product_name    VARCHAR(255),
    quantity        INTEGER      NOT NULL,
    price_at_purchase NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
