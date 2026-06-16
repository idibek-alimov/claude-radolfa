-- Strip the legacy "RD-" prefix from article codes; codes become plain digits.
UPDATE listing_variants
SET product_code = regexp_replace(product_code, '^RD-', '')
WHERE product_code LIKE 'RD-%';
