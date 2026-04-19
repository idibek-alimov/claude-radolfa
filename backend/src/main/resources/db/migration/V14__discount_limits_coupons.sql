ALTER TABLE discounts ADD COLUMN min_basket_amount      NUMERIC(12,2) NULL;
ALTER TABLE discounts ADD COLUMN usage_cap_total        INT           NULL;
ALTER TABLE discounts ADD COLUMN usage_cap_per_customer INT           NULL;
ALTER TABLE discounts ADD COLUMN coupon_code            VARCHAR(64)   NULL UNIQUE;
