-- ============================================================
-- V11: Soft delete for products and orders
--
-- Adds a nullable deleted_at column. NULL = active row.
-- Partial indexes ensure active-row queries stay fast.
-- ============================================================

-- Products
ALTER TABLE products ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_products_active ON products (id)
    WHERE deleted_at IS NULL;

-- Orders
ALTER TABLE orders ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_orders_active ON orders (id)
    WHERE deleted_at IS NULL;
