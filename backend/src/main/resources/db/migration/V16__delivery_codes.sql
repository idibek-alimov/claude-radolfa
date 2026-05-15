-- ----------------------------------------------------------------
-- Delivery / pickup verification codes
-- ----------------------------------------------------------------
-- One-time 6-digit codes sent to customers at handoff events:
--   HOME delivery  → generated when order moves to SHIPPED
--   PICKPOINT      → generated when order moves to READY_FOR_PICKUP
-- The courier or pickpoint staff enters the code to confirm handoff.
-- Superseded codes are marked used_at; expired codes are detected
-- by comparing expires_at against the current timestamp.
-- ----------------------------------------------------------------
CREATE TABLE delivery_codes (
    id            BIGSERIAL     PRIMARY KEY,
    order_id      BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    code          VARCHAR(6)    NOT NULL,
    expires_at    TIMESTAMPTZ   NOT NULL,
    used_at       TIMESTAMPTZ,
    attempt_count INT           NOT NULL DEFAULT 0,
    version       BIGINT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_codes_order_id ON delivery_codes (order_id);
