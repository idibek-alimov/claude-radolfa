-- ================================================================
-- V14__cart.sql
--
-- Persistent server-side cart. One cart per user (UNIQUE on user_id).
-- Cart items reference skus(id) for SKU-level granularity.
-- ================================================================

CREATE TABLE carts (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE REFERENCES users(id),
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id              BIGSERIAL       PRIMARY KEY,
    cart_id         BIGINT          NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    sku_id          BIGINT          NOT NULL REFERENCES skus(id),
    listing_slug    VARCHAR(255)    NOT NULL,
    product_name    VARCHAR(255),
    size_label      VARCHAR(64),
    image_url       TEXT,
    price_snapshot  NUMERIC(12, 2)  NOT NULL,
    quantity        INTEGER         NOT NULL DEFAULT 1,
    UNIQUE(cart_id, sku_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
