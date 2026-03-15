-- ============================================================
-- V20_1__seed_discounts_data.sql
--
-- DEV ONLY — Seeds the discounts table with pricing rules
-- that match the scenarios originally in V17_1.
--
-- Discount scenarios:
--   Group 2 (products 7-12):  15% active, no expiry
--   Group 3 (products 13-18): 25% active, future expiry
--   Group 4 (products 19-22): 30% expired (past valid_upto)
--   Group 5 (products 23-26): 10% active, no expiry
-- ============================================================

-- ----------------------------------------------------------------
-- Group 2: 15% ERP discount (active, no expiry)
-- ----------------------------------------------------------------
INSERT INTO discounts (erp_pricing_rule_id, item_code, discount_value, valid_from, valid_upto, is_disabled)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    s.erp_item_code,
    15.00,
    NOW() - INTERVAL '30 days',
    NOW() + INTERVAL '365 days',
    FALSE
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SUIT-001', 'TPL-SWEATER-001', 'TPL-CARDGN-001',
    'TPL-BLAZER-001', 'TPL-TRENCH-001', 'TPL-SHORTS-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

-- ----------------------------------------------------------------
-- Group 3: 25% ERP discount with future expiry (14 days)
-- ----------------------------------------------------------------
INSERT INTO discounts (erp_pricing_rule_id, item_code, discount_value, valid_from, valid_upto, is_disabled)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    s.erp_item_code,
    25.00,
    NOW() - INTERVAL '7 days',
    NOW() + INTERVAL '14 days',
    FALSE
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SWIM-001', 'TPL-PAJAMA-001', 'TPL-POLO-001',
    'TPL-LINEN-001', 'TPL-JOGGER-001', 'TPL-FLEECE-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

-- ----------------------------------------------------------------
-- Group 4: 30% EXPIRED discount (valid_upto in the past)
-- ----------------------------------------------------------------
INSERT INTO discounts (erp_pricing_rule_id, item_code, discount_value, valid_from, valid_upto, is_disabled)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    s.erp_item_code,
    30.00,
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '3 days',
    FALSE
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-TIE-001', 'TPL-SCARF-001', 'TPL-SNEAK-001', 'TPL-BOOTS-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

-- ----------------------------------------------------------------
-- Group 5: 10% ERP discount (mild, no expiry)
-- ----------------------------------------------------------------
INSERT INTO discounts (erp_pricing_rule_id, item_code, discount_value, valid_from, valid_upto, is_disabled)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    s.erp_item_code,
    10.00,
    NOW() - INTERVAL '30 days',
    NOW() + INTERVAL '365 days',
    FALSE
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SANDAL-001', 'TPL-CAP-001', 'TPL-BKPCK-001', 'TPL-WALLET-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;
