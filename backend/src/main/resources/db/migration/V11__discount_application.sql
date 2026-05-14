-- ================================================================
-- V11__discount_application.sql
--
-- Records one row per order line where a discount campaign was the
-- winning price source at order confirmation. Populated forward-only
-- by CheckoutService inside the checkout @Transactional boundary.
-- Phase 8 will query this table for campaign-level KPIs.
-- ================================================================

CREATE TABLE discount_application (
    id                   BIGSERIAL     PRIMARY KEY,
    discount_id          BIGINT        NOT NULL REFERENCES discounts(id),
    order_id             BIGINT        NOT NULL REFERENCES orders(id)      ON DELETE CASCADE,
    order_line_id        BIGINT        NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    sku_item_code        VARCHAR(128)  NOT NULL,
    quantity             INT           NOT NULL CHECK (quantity > 0),
    original_unit_price  NUMERIC(12,2) NOT NULL,
    applied_unit_price   NUMERIC(12,2) NOT NULL,
    discount_amount      NUMERIC(12,2) NOT NULL,
    applied_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_discount_app_discount_time ON discount_application (discount_id, applied_at);
CREATE INDEX idx_discount_app_order         ON discount_application (order_id);
