-- ================================================================
-- V7__discounts.sql
--
-- Pricing Rule discounts synced from ERPNext.
-- One discount can apply to many item codes via join table.
-- Active check: is_disabled = false AND valid_from <= NOW() <= valid_upto
-- ================================================================

CREATE TABLE discounts (
    id                  BIGSERIAL       PRIMARY KEY,
    erp_pricing_rule_id VARCHAR(140)    NOT NULL UNIQUE,
    discount_value      NUMERIC(5, 2)   NOT NULL,
    valid_from          TIMESTAMPTZ     NOT NULL,
    valid_upto          TIMESTAMPTZ     NOT NULL,
    is_disabled         BOOLEAN         NOT NULL DEFAULT FALSE,
    title               VARCHAR(255),
    color_hex           VARCHAR(7),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE discount_items (
    discount_id BIGINT      NOT NULL REFERENCES discounts(id) ON DELETE CASCADE,
    item_code   VARCHAR(64) NOT NULL,
    CONSTRAINT pk_discount_items PRIMARY KEY (discount_id, item_code)
);

CREATE INDEX idx_discount_items_item_code ON discount_items (item_code);
CREATE INDEX idx_discounts_active ON discounts (is_disabled, valid_from, valid_upto);
