-- ─────────────────────────────────────────────────────────────────────────────
-- Warehouse location system: Zone → Shelf → Bin, plus SKU → Bin assignment.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE warehouse_zones (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    label      VARCHAR(100),
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouse_shelves (
    id         BIGSERIAL    PRIMARY KEY,
    zone_id    BIGINT       NOT NULL REFERENCES warehouse_zones(id) ON DELETE CASCADE,
    code       VARCHAR(20)  NOT NULL,
    label      VARCHAR(100),
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (zone_id, code)
);

CREATE TABLE warehouse_bins (
    id         BIGSERIAL    PRIMARY KEY,
    shelf_id   BIGINT       NOT NULL REFERENCES warehouse_shelves(id) ON DELETE CASCADE,
    code       VARCHAR(20)  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (shelf_id, code)
);

-- Prod: adds bin_id to the existing skus table with the FK now that warehouse_bins exists.
-- Dev fresh install: bin_id was already added in V2 (no FK, since warehouse_bins didn't
-- exist at V2 time) — IF NOT EXISTS makes this a no-op so the migration still passes.
ALTER TABLE skus
    ADD COLUMN IF NOT EXISTS bin_id BIGINT REFERENCES warehouse_bins(id) ON DELETE SET NULL;

CREATE INDEX idx_skus_bin_id      ON skus(bin_id);
CREATE INDEX idx_shelves_zone_id  ON warehouse_shelves(zone_id);
CREATE INDEX idx_bins_shelf_id    ON warehouse_bins(shelf_id);
