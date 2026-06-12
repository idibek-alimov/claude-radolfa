CREATE TABLE inventory_placements (
    id           BIGSERIAL    PRIMARY KEY,
    sku_id       BIGINT       NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    warehouse_id BIGINT       NOT NULL DEFAULT 1 REFERENCES warehouses(id) ON DELETE RESTRICT,
    bin_id       BIGINT       REFERENCES warehouse_bins(id) ON DELETE RESTRICT,  -- NULL = inbound pool
    quantity     INTEGER      NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- One inbound (unassigned) row per (sku, warehouse)
CREATE UNIQUE INDEX uq_placement_inbound
    ON inventory_placements (sku_id, warehouse_id)
    WHERE bin_id IS NULL;

-- One placed row per (sku, warehouse, bin)
CREATE UNIQUE INDEX uq_placement_bin
    ON inventory_placements (sku_id, warehouse_id, bin_id)
    WHERE bin_id IS NOT NULL;

CREATE INDEX idx_placements_sku       ON inventory_placements (sku_id);
CREATE INDEX idx_placements_warehouse ON inventory_placements (warehouse_id);
CREATE INDEX idx_placements_bin       ON inventory_placements (bin_id);

-- Backfill: every SKU with existing stock starts entirely in the inbound pool
-- of the default warehouse (id 1). Mirror (skus.stock_quantity) already equals
-- this sum, so no mirror rewrite is needed here.
INSERT INTO inventory_placements (sku_id, warehouse_id, bin_id, quantity)
SELECT id, 1, NULL, stock_quantity
FROM skus
WHERE stock_quantity > 0;
