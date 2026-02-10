-- ================================================================
-- V13__cleanup_legacy_schema.sql
--
-- Removes legacy 'products' and 'product_images' tables.
-- Updates 'order_items' to reference 'skus' instead of 'products'.
--
-- NOTE: This migration TRUNCATES orders data because IDs cannot
-- be migrated reliably between the old products table and new
-- sku/variant tables.
-- ================================================================

-- 1. Truncate orders and items to allow schema change without data conflicts
TRUNCATE TABLE order_items CASCADE;
TRUNCATE TABLE orders CASCADE;

-- 2. Drop legacy tables
DROP TABLE IF EXISTS product_images CASCADE;
DROP TABLE IF EXISTS products CASCADE;

-- 3. Update order_items to reference skus instead of products
--    We drop the column because the old IDs are meaningless for SKUs
ALTER TABLE order_items DROP COLUMN IF EXISTS product_id;

--    Add the new column
ALTER TABLE order_items ADD COLUMN sku_id BIGINT NOT NULL;

--    Add index and foreign key constraint
CREATE INDEX idx_order_items_sku_id ON order_items(sku_id);

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_sku
    FOREIGN KEY (sku_id)
    REFERENCES skus (id);

-- 4. Add top_selling to listing_variants
ALTER TABLE listing_variants ADD COLUMN IF NOT EXISTS top_selling BOOLEAN NOT NULL DEFAULT FALSE;

