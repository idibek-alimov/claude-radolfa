-- Phase 1b: Add logistics fields to skus table
ALTER TABLE skus
    ADD COLUMN barcode    VARCHAR(128),
    ADD COLUMN weight_kg  DOUBLE PRECISION,
    ADD COLUMN width_cm   INTEGER,
    ADD COLUMN height_cm  INTEGER,
    ADD COLUMN depth_cm   INTEGER;

ALTER TABLE skus ADD CONSTRAINT uq_skus_barcode UNIQUE (barcode);
