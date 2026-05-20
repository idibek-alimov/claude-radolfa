CREATE TABLE stock_receipts (
    id                  BIGSERIAL    PRIMARY KEY,
    created_by_user_id  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    supplier_reference  VARCHAR(200),
    notes               TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_receipt_items (
    id                  BIGSERIAL    PRIMARY KEY,
    receipt_id          BIGINT       NOT NULL REFERENCES stock_receipts(id) ON DELETE CASCADE,
    sku_id              BIGINT       REFERENCES skus(id) ON DELETE SET NULL,
    sku_code            VARCHAR(64)  NOT NULL,
    product_name        VARCHAR(255) NOT NULL,
    quantity_received   INT          NOT NULL CHECK (quantity_received > 0),
    notes               TEXT,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_receipts_created_by  ON stock_receipts(created_by_user_id);
CREATE INDEX idx_stock_receipts_created_at  ON stock_receipts(created_at DESC);
CREATE INDEX idx_stock_receipt_items_receipt ON stock_receipt_items(receipt_id);
CREATE INDEX idx_stock_receipt_items_sku    ON stock_receipt_items(sku_id);
