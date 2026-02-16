-- ============================================================
-- V13: Add denormalized category_name to product_bases
--
-- Preserves category name even if the category entity is deleted.
-- Kept in sync by the application layer (ProductHierarchyAdapter).
-- ============================================================

ALTER TABLE product_bases ADD COLUMN category_name VARCHAR(255);

-- Backfill from existing category FK
UPDATE product_bases pb
SET category_name = c.name
FROM categories c
WHERE pb.category_id = c.id;
