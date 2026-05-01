-- Per-trait aggregated scores precomputed alongside the star summary.
-- Stored as a JSONB array of {traitKey, labelI18n, inputType, average, count} objects.
ALTER TABLE product_rating_summaries
    ADD COLUMN trait_aggregates JSONB DEFAULT '[]'::jsonb;
