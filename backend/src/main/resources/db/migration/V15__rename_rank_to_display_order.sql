-- ============================================================
-- V15: Rename rank to display_order for clarity
-- ============================================================

ALTER TABLE loyalty_tiers RENAME COLUMN rank TO display_order;

ALTER INDEX idx_loyalty_tiers_rank RENAME TO idx_loyalty_tiers_display_order;
