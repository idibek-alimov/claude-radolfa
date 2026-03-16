-- ================================================================
-- V7_1__seed_discounts_data.sql
--
-- DEV ONLY — Seeds the discounts + discount_items tables with
-- pricing rules that match product variants from V6_1.
--
-- Discount scenarios:
--   Group A (templates 1-3):  15% "Winter Collection" (active, blue)
--   Group B (templates 4-5):  25% "Flash Sale" (active 14 days, red)
--   Group C (templates 6-7):  30% "End of Season" (expired, grey)
--   Group D (templates 8-9):  10% "New Members" (active, violet)
--   Group E (template 10):    No discount
--
-- Also seeds a group discount: "Summer Bundle" 20% covering
-- templates 4 + 8 (cross-category).
-- ================================================================

-- ----------------------------------------------------------------
-- Per-item discounts
-- ----------------------------------------------------------------

-- Group A: 15% Winter Collection (templates 1-3)
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex, version, created_at, updated_at)
VALUES ('PRC-RULE-WINTER', 15.00, NOW() - INTERVAL '30 days', NOW() + INTERVAL '365 days', FALSE, 'Winter Collection', '#3B82F6', 0, NOW(), NOW())
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, v.erp_variant_code
FROM discounts d, product_variants v
JOIN product_templates t ON v.template_id = t.id
WHERE d.erp_pricing_rule_id = 'PRC-RULE-WINTER'
  AND t.erp_template_code IN ('TPL-TSHIRT-001', 'TPL-HOODIE-001', 'TPL-JEANS-001')
ON CONFLICT DO NOTHING;

-- Group B: 25% Flash Sale (templates 4-5, 14 days)
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex, version, created_at, updated_at)
VALUES ('PRC-RULE-FLASH', 25.00, NOW() - INTERVAL '7 days', NOW() + INTERVAL '14 days', FALSE, 'Flash Sale', '#EF4444', 0, NOW(), NOW())
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, v.erp_variant_code
FROM discounts d, product_variants v
JOIN product_templates t ON v.template_id = t.id
WHERE d.erp_pricing_rule_id = 'PRC-RULE-FLASH'
  AND t.erp_template_code IN ('TPL-WINDBRK-001', 'TPL-DRESS-001')
ON CONFLICT DO NOTHING;

-- Group C: 30% End of Season EXPIRED (templates 6-7)
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex, version, created_at, updated_at)
VALUES ('PRC-RULE-ENDSEASON', 30.00, NOW() - INTERVAL '30 days', NOW() - INTERVAL '3 days', FALSE, 'End of Season', '#6B7280', 0, NOW(), NOW())
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, v.erp_variant_code
FROM discounts d, product_variants v
JOIN product_templates t ON v.template_id = t.id
WHERE d.erp_pricing_rule_id = 'PRC-RULE-ENDSEASON'
  AND t.erp_template_code IN ('TPL-POLO-001', 'TPL-JOGGER-001')
ON CONFLICT DO NOTHING;

-- Group D: 10% New Members (templates 8-9)
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex, version, created_at, updated_at)
VALUES ('PRC-RULE-NEWMEMBER', 10.00, NOW() - INTERVAL '30 days', NOW() + INTERVAL '365 days', FALSE, 'New Members', '#8B5CF6', 0, NOW(), NOW())
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, v.erp_variant_code
FROM discounts d, product_variants v
JOIN product_templates t ON v.template_id = t.id
WHERE d.erp_pricing_rule_id = 'PRC-RULE-NEWMEMBER'
  AND t.erp_template_code IN ('TPL-SNEAK-001', 'TPL-CAP-001')
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------
-- Group discount: "Summer Bundle" 20% (cross-category: templates 4+8)
-- ----------------------------------------------------------------
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex, version, created_at, updated_at)
VALUES ('PRC-RULE-SUMMER-BUNDLE', 20.00, NOW() - INTERVAL '3 days', NOW() + INTERVAL '30 days', FALSE, 'Summer Bundle', '#10B981', 0, NOW(), NOW())
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, v.erp_variant_code
FROM discounts d, product_variants v
JOIN product_templates t ON v.template_id = t.id
WHERE d.erp_pricing_rule_id = 'PRC-RULE-SUMMER-BUNDLE'
  AND t.erp_template_code IN ('TPL-WINDBRK-001', 'TPL-SNEAK-001')
ON CONFLICT DO NOTHING;
