-- ================================================================
-- V37__loyalty_ledger.sql
--
-- Append-only loyalty points ledger. Single table that doubles as:
--   * FIFO lot store  — credit rows (delta > 0) carry remaining_points
--                       and expires_at; consumed oldest-first.
--   * audit trail     — every movement is a row (one debit row per lot
--                       consumed).
--
-- Invariant: a user's balance equals
--   SUM(delta)  ==  SUM(remaining_points WHERE delta > 0)
-- and is cached on users.loyalty_points (kept in sync in the same tx).
-- ================================================================

CREATE TABLE loyalty_ledger (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    delta            INTEGER      NOT NULL,          -- signed: + credit, - debit
    reason           VARCHAR(24)  NOT NULL,          -- LoyaltyReason
    order_id         BIGINT       REFERENCES orders(id) ON DELETE SET NULL,
    actor_user_id    BIGINT       REFERENCES users(id) ON DELETE SET NULL, -- manual adjust
    source_lot_id    BIGINT       REFERENCES loyalty_ledger(id),  -- debit -> credit row drawn from
    remaining_points INTEGER,                        -- credit rows only; decremented on consume/expire
    expires_at       TIMESTAMPTZ,                    -- credit rows only; NULL = never
    balance_after    INTEGER      NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Drawer read: a user's movements, newest first.
CREATE INDEX idx_loyalty_ledger_user_time ON loyalty_ledger (user_id, created_at DESC);

-- FIFO live-lot scan: positive credit rows with remaining, oldest first.
CREATE INDEX idx_loyalty_ledger_live_lots ON loyalty_ledger (user_id, created_at)
    WHERE delta > 0 AND remaining_points > 0;

-- Expiry sweep: live lots past their expiry.
CREATE INDEX idx_loyalty_ledger_expiry ON loyalty_ledger (expires_at)
    WHERE delta > 0 AND remaining_points > 0;
