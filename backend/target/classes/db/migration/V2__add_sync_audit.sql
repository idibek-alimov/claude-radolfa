-- ================================================================
-- V2__add_sync_audit.sql
-- Adds last_erp_sync_at audit column to products and creates the
-- erp_sync_log table for per-row sync event tracking.
-- ================================================================

-- ----------------------------------------------------------------
-- products – audit timestamp
-- ----------------------------------------------------------------
ALTER TABLE products
    ADD COLUMN last_erp_sync_at TIMESTAMPTZ;

-- ----------------------------------------------------------------
-- erp_sync_log – one row per sync attempt per product
-- ----------------------------------------------------------------
CREATE TABLE erp_sync_log (
    id            BIGSERIAL   PRIMARY KEY,
    erp_id        VARCHAR(64) NOT NULL,
    synced_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status        VARCHAR(16) NOT NULL CHECK (status IN ('SUCCESS', 'ERROR')),
    error_message TEXT
);

CREATE INDEX idx_erp_sync_log_erp_id    ON erp_sync_log (erp_id);
CREATE INDEX idx_erp_sync_log_synced_at ON erp_sync_log (synced_at);
