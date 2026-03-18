-- ============================================================
-- V14: Create loyalty_tiers table and add tier columns to users
--
-- Includes: display_order naming (was V15 rename), color column (was V16)
-- ============================================================

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

ALTER TABLE users ADD COLUMN tier_id               BIGINT       REFERENCES loyalty_tiers(id);
ALTER TABLE users ADD COLUMN spend_to_next_tier    NUMERIC(12,2);
ALTER TABLE users ADD COLUMN spend_to_maintain_tier NUMERIC(12,2);
ALTER TABLE users ADD COLUMN current_month_spending NUMERIC(12,2);
