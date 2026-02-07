-- ============================================================
-- V8: Add optimistic-locking version columns
-- Required by BaseAuditEntity (@Version)
-- ============================================================

ALTER TABLE products
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
