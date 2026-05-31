-- ================================================================
-- V27__sellers.sql
--
-- Marketplace seller support (Phase 1 of minimal marketplace).
--
-- Creates:
--   sellers — one row per marketplace seller (shop identity)
--
-- Also adds dormant FK columns on existing tables (cannot live in
-- V2/V3 because the sellers table does not exist when those run):
--   product_bases.seller_id  (used from Phase 2 onward)
--   order_items.seller_id    (used from Phase 5 onward)
-- ================================================================

CREATE TABLE sellers (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    shop_name  VARCHAR(120) NOT NULL,
    logo_url   VARCHAR(500),
    bio        TEXT,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sellers_user_id ON sellers(user_id);

-- Dormant until Phase 2 — product ownership
ALTER TABLE product_bases
    ADD COLUMN seller_id BIGINT REFERENCES sellers(id) ON DELETE SET NULL;

CREATE INDEX idx_product_bases_seller_id ON product_bases(seller_id);

-- Dormant until Phase 5 — order item attribution
ALTER TABLE order_items
    ADD COLUMN seller_id BIGINT REFERENCES sellers(id) ON DELETE SET NULL;

CREATE INDEX idx_order_items_seller_id ON order_items(seller_id);
