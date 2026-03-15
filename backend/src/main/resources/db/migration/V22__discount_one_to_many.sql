-- ============================================================
-- V22__discount_one_to_many.sql
--
-- Refactors discounts from 1:1 (one discount → one SKU item_code)
-- to 1:N (one discount → many item codes via join table).
--
-- Also adds display attributes: title and color_hex.
-- ============================================================

-- 1. Create join table BEFORE dropping item_code so we can migrate data
CREATE TABLE discount_items (
    discount_id BIGINT      NOT NULL REFERENCES discounts(id) ON DELETE CASCADE,
    item_code   VARCHAR(64) NOT NULL,
    CONSTRAINT pk_discount_items PRIMARY KEY (discount_id, item_code)
);

CREATE INDEX idx_discount_items_item_code ON discount_items (item_code);

-- 2. Migrate existing 1:1 data into the new join table
INSERT INTO discount_items (discount_id, item_code)
SELECT id, item_code FROM discounts;

-- 3. Add new display-only columns (nullable — populated via ERP sync going forward)
ALTER TABLE discounts ADD COLUMN title     VARCHAR(255);
ALTER TABLE discounts ADD COLUMN color_hex VARCHAR(7);

-- 4. Drop the now-redundant item_code column from the parent table
ALTER TABLE discounts DROP COLUMN item_code;
