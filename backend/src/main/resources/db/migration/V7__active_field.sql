-- V7: Add 'active' flag to listing_variants.
-- New products default to inactive so managers can prepare catalog before going live.
ALTER TABLE listing_variants
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE;

-- Activate all existing products (they were already public).
UPDATE listing_variants SET active = TRUE;
