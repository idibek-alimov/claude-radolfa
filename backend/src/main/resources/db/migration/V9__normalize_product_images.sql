-- ============================================================
-- V9: Normalize product images
-- Replaces the Postgres-specific TEXT[] array column with a
-- proper join table, enabling indexing, per-image metadata,
-- and database portability.
-- ============================================================

-- 1. Create the normalized table
CREATE TABLE product_images (
    id         BIGSERIAL    PRIMARY KEY,
    product_id BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url  TEXT         NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- 2. Migrate existing array data (preserving order via WITH ORDINALITY)
INSERT INTO product_images (product_id, image_url, sort_order)
SELECT p.id,
       img.url,
       img.ord::INT - 1
FROM   products p,
       LATERAL unnest(p.images) WITH ORDINALITY AS img(url, ord)
WHERE  p.images IS NOT NULL;

-- 3. Drop the legacy array column
ALTER TABLE products DROP COLUMN images;
