CREATE TABLE inventory_transactions (
    id             BIGSERIAL    PRIMARY KEY,
    sku_id         BIGINT       NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    delta          INTEGER      NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    reference_type VARCHAR(30),
    reference_id   BIGINT,
    actor_user_id  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    notes          TEXT,
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
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
