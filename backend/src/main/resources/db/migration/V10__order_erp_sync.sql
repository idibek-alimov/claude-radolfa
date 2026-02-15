-- Add ERP reference fields for order synchronization from ERPNext.
-- erp_order_id: unique ERPNext Sales Order ID for deduplication.
-- erp_item_code: ERPNext item code on line items (orders may reference items not in local SKU table).
-- sku_id becomes nullable since ERP-synced orders may not map to a local SKU.

ALTER TABLE orders
    ADD COLUMN erp_order_id VARCHAR(64) UNIQUE;

CREATE INDEX idx_orders_erp_order_id ON orders (erp_order_id) WHERE erp_order_id IS NOT NULL;

ALTER TABLE order_items
    ADD COLUMN erp_item_code VARCHAR(128);

ALTER TABLE order_items
    ALTER COLUMN sku_id DROP NOT NULL;

ALTER TABLE order_items
    DROP CONSTRAINT order_items_sku_id_fkey;

ALTER TABLE order_items
    ADD CONSTRAINT order_items_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES skus(id) ON DELETE SET NULL;
