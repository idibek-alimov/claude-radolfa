-- ================================================================
-- V7__add_featured_flag.sql
--
-- Adds a "featured" flag to listing_variants for homepage curation.
-- Partial index covers only TRUE rows (the typical query pattern).
-- ================================================================

ALTER TABLE listing_variants ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_listing_variants_featured ON listing_variants (featured) WHERE featured = TRUE;
