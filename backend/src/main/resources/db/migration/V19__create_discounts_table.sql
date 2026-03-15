-- ================================================================
-- V19__create_discounts_table.sql
--
-- Pricing Rule discounts synced from ERPNext.
-- Active check: is_disabled = false AND valid_from <= NOW() <= valid_upto
-- ================================================================

CREATE TABLE discounts (
    id             BIGSERIAL       PRIMARY KEY,
    erp_pricing_rule_id VARCHAR(140) NOT NULL UNIQUE,
    item_code      VARCHAR(64)     NOT NULL,
    discount_value NUMERIC(5, 2)   NOT NULL,
    valid_from     TIMESTAMPTZ     NOT NULL,
    valid_upto     TIMESTAMPTZ     NOT NULL,
    is_disabled    BOOLEAN         NOT NULL DEFAULT FALSE,
    version        BIGINT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_discounts_item_code ON discounts (item_code);
CREATE INDEX idx_discounts_active ON discounts (item_code, is_disabled, valid_from, valid_upto);
