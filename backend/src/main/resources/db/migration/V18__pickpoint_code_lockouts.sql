-- ----------------------------------------------------------------
-- Per-pickpoint rate-limit lockout table
-- ----------------------------------------------------------------
-- Tracks consecutive failed verification attempts at each pickup
-- point. Once failed_count reaches the configured threshold the
-- lockout window is set; any verification attempt during an active
-- lockout is rejected with 429 Too Many Requests without loading
-- the delivery code at all.
-- ----------------------------------------------------------------
CREATE TABLE pickpoint_code_lockouts (
    id             BIGSERIAL    PRIMARY KEY,
    pickpoint_id   BIGINT       NOT NULL REFERENCES pickpoint(id) ON DELETE CASCADE,
    locked_until   TIMESTAMPTZ,
    failed_count   INT          NOT NULL DEFAULT 0,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_pickpoint_code_lockouts_pickpoint ON pickpoint_code_lockouts(pickpoint_id);
CREATE INDEX idx_pickpoint_code_lockouts_until ON pickpoint_code_lockouts(locked_until);
