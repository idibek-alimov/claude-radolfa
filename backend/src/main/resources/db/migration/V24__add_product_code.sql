-- Human-friendly product identifier for storefront use.
-- Format: RD-XXXXX (e.g. RD-10001). Shown on every product card.
-- Users can type this into the search box to navigate directly to a product.

CREATE SEQUENCE listing_variant_code_seq START WITH 10001 INCREMENT BY 1;

ALTER TABLE listing_variants
    ADD COLUMN product_code VARCHAR(10) DEFAULT NULL;

CREATE UNIQUE INDEX uq_listing_variant_product_code
    ON listing_variants (product_code);

-- Backfill existing dev rows so every card already has a code.
UPDATE listing_variants
SET product_code = 'RD-' || LPAD(NEXTVAL('listing_variant_code_seq')::TEXT, 5, '0')
WHERE product_code IS NULL;
