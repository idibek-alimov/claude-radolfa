-- ================================================================
-- V6__loyalty_tiers.sql
--
-- Loyalty tier definitions + FK from users.
-- ================================================================

CREATE TABLE loyalty_tiers (
    id                    BIGSERIAL      PRIMARY KEY,
    name                  VARCHAR(50)    NOT NULL UNIQUE,
    discount_percentage   NUMERIC(5,2)   NOT NULL DEFAULT 0,
    cashback_percentage   NUMERIC(5,2)   NOT NULL DEFAULT 0,
    min_spend_requirement NUMERIC(12,2)  NOT NULL DEFAULT 0,
    display_order         INT            NOT NULL DEFAULT 0,
    color                 VARCHAR(7)     NOT NULL DEFAULT '#6366F1',
    version               BIGINT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loyalty_tiers_display_order ON loyalty_tiers(display_order);

-- Wire up deferred FK from V1
ALTER TABLE users ADD CONSTRAINT fk_users_tier FOREIGN KEY (tier_id) REFERENCES loyalty_tiers(id);
