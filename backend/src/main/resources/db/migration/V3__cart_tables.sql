-- Phase 6: Cart lifecycle tables

CREATE TABLE carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version    BIGINT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id                  BIGSERIAL    PRIMARY KEY,
    cart_id             BIGINT       NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    sku_id              BIGINT       NOT NULL REFERENCES skus(id),
    quantity            INT          NOT NULL CHECK (quantity > 0),
    unit_price_snapshot NUMERIC(12,2) NOT NULL,
    added_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (cart_id, sku_id)
);

CREATE INDEX idx_carts_user_status ON carts(user_id, status);
