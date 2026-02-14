-- ============================================================
-- V8: Relational Categories & Colors
--
-- Promotes flat String fields (category, color_key) to
-- first-class entities with FK relationships.
-- ============================================================

-- 1. Create lookup tables
CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(128) NOT NULL UNIQUE,
    slug       VARCHAR(128) NOT NULL UNIQUE,
    parent_id  BIGINT REFERENCES categories(id),
    version    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE colors (
    id           BIGSERIAL PRIMARY KEY,
    color_key    VARCHAR(64)  NOT NULL UNIQUE,
    display_name VARCHAR(128),
    hex_code     VARCHAR(7),
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

-- 2. Seed from existing data
INSERT INTO categories (name, slug)
SELECT DISTINCT category, LOWER(REPLACE(category, ' ', '-'))
FROM product_bases
WHERE category IS NOT NULL;

INSERT INTO colors (color_key, display_name)
SELECT DISTINCT color_key, REPLACE(color_key, '-', ' ')
FROM listing_variants;

-- 3. Add FK columns
ALTER TABLE product_bases ADD COLUMN category_id BIGINT REFERENCES categories(id);
ALTER TABLE listing_variants ADD COLUMN color_id BIGINT REFERENCES colors(id);

-- 4. Backfill FKs from existing string data
UPDATE product_bases pb
SET category_id = c.id
FROM categories c
WHERE pb.category = c.name;

UPDATE listing_variants lv
SET color_id = cl.id
FROM colors cl
WHERE lv.color_key = cl.color_key;

-- 5. Make color_id NOT NULL, drop old columns
ALTER TABLE listing_variants ALTER COLUMN color_id SET NOT NULL;
ALTER TABLE product_bases DROP COLUMN category;
ALTER TABLE listing_variants DROP COLUMN color_key;

-- 6. Update unique constraint
ALTER TABLE listing_variants DROP CONSTRAINT IF EXISTS listing_variants_product_base_id_color_key_key;
ALTER TABLE listing_variants ADD CONSTRAINT uq_variant_base_color UNIQUE (product_base_id, color_id);
