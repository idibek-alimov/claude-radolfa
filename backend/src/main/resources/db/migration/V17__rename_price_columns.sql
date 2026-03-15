-- Rename price columns for semantic clarity:
--   price       -> original_price    (MRP / standard rate from ERP)
--   sale_price  -> discounted_price  (after ERP product-level discount, nullable)
--   sale_ends_at -> discounted_ends_at

ALTER TABLE skus RENAME COLUMN price TO original_price;
ALTER TABLE skus RENAME COLUMN sale_price TO discounted_price;
ALTER TABLE skus RENAME COLUMN sale_ends_at TO discounted_ends_at;
