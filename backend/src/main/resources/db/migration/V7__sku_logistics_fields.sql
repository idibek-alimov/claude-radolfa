-- Phase 1b: Add logistics fields to skus table.
-- These columns are also included in the V1 baseline for fresh installs.
-- IF NOT EXISTS guards make this migration safe on both fresh and upgraded databases.
ALTER TABLE skus
    -- barcode is intentionally nullable: ERP-synced SKUs arrive without a barcode
    -- and are inserted via a separate import path that bypasses the creation DTO.
    -- The CreateProductRequestDto enforces @NotBlank at the API level for native creation.
    -- Do NOT add NOT NULL here — it would break ERP sync.
    ADD COLUMN IF NOT EXISTS barcode    VARCHAR(128),
    ADD COLUMN IF NOT EXISTS weight_kg  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS width_cm   INTEGER,
    ADD COLUMN IF NOT EXISTS height_cm  INTEGER,
    ADD COLUMN IF NOT EXISTS depth_cm   INTEGER;

ALTER TABLE skus DROP CONSTRAINT IF EXISTS uq_skus_barcode;
ALTER TABLE skus ADD CONSTRAINT uq_skus_barcode UNIQUE (barcode);
