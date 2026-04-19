ALTER TABLE discounts ADD COLUMN amount_type VARCHAR(16) NOT NULL DEFAULT 'PERCENT';
ALTER TABLE discounts ALTER COLUMN discount_value TYPE NUMERIC(12,2);
ALTER TABLE discounts RENAME COLUMN discount_value TO amount_value;
