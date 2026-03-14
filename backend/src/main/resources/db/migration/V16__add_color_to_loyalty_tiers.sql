-- ============================================================
-- V16: Add color column to loyalty_tiers
-- ============================================================
ALTER TABLE loyalty_tiers ADD COLUMN color VARCHAR(7) NOT NULL DEFAULT '#6366F1';
