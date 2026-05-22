CREATE TABLE warehouses (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_warehouses_one_default ON warehouses(is_default) WHERE is_default = TRUE;

INSERT INTO warehouses (code, name, is_default)
VALUES ('MAIN', 'Main Warehouse', TRUE);

CREATE TABLE inventory_transactions (
    id             BIGSERIAL    PRIMARY KEY,
    sku_id         BIGINT       NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    delta          INTEGER      NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    reference_type VARCHAR(30),
    reference_id   BIGINT,
    actor_user_id  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    notes          TEXT,
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    warehouse_id   BIGINT       NOT NULL DEFAULT 1 REFERENCES warehouses(id) ON DELETE RESTRICT
);

CREATE INDEX idx_inv_tx_sku_id      ON inventory_transactions(sku_id);
CREATE INDEX idx_inv_tx_type        ON inventory_transactions(type);
CREATE INDEX idx_inv_tx_occurred_at ON inventory_transactions(occurred_at DESC);
CREATE INDEX idx_inv_tx_reference   ON inventory_transactions(reference_type, reference_id);

-- Opening-balance backfill: one RECEIPT row per SKU that currently has stock > 0.
-- Represents the snapshot so the ledger starts consistent.
INSERT INTO inventory_transactions (sku_id, delta, type, reference_type, notes, occurred_at)
SELECT id, stock_quantity, 'RECEIPT', 'INITIAL_BACKFILL',
       'Opening balance at ledger introduction', NOW()
FROM skus
WHERE stock_quantity > 0;
