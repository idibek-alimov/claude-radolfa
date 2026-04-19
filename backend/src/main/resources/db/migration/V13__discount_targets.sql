CREATE TABLE discount_target (
  id                  BIGSERIAL    PRIMARY KEY,
  discount_id         BIGINT       NOT NULL REFERENCES discounts(id) ON DELETE CASCADE,
  target_type         VARCHAR(16)  NOT NULL,
  reference_id        VARCHAR(128) NOT NULL,
  include_descendants BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_discount_target_discount ON discount_target (discount_id);
CREATE INDEX idx_discount_target_ref ON discount_target (target_type, reference_id);
INSERT INTO discount_target (discount_id, target_type, reference_id)
SELECT discount_id, 'SKU', item_code FROM discount_items;
