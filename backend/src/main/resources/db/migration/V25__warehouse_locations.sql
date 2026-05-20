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

-- skus is created back in V2 — warehouse_bins didn't exist then, so the FK
-- must be added now as an ALTER. ON DELETE SET NULL means deleting a bin
-- leaves orphaned SKUs (their bin_id becomes NULL) rather than blocking deletion.
ALTER TABLE skus
    ADD COLUMN bin_id BIGINT REFERENCES warehouse_bins(id) ON DELETE SET NULL;

CREATE INDEX idx_skus_bin_id      ON skus(bin_id);
CREATE INDEX idx_shelves_zone_id  ON warehouse_shelves(zone_id);
CREATE INDEX idx_bins_shelf_id    ON warehouse_bins(shelf_id);
