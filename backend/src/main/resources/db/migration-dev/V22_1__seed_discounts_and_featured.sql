-- ============================================================
-- V22_1__seed_discounts_and_featured.sql
--
-- DEV ONLY — Consolidated discount + featured seed.
-- Runs after V22 (discount_items join table exists).
--
-- Consolidates:
--   - featured flag updates (was V17_1)
--   - discounts table seed  (was V20_1)
--   - discount titles + group discounts (was V22_1)
--
-- Discount scenarios:
--   Group 2 (products 7-12):  15% "Winter Collection", active, no expiry
--   Group 3 (products 13-18): 25% "Flash Sale", active, future expiry
--   Group 4 (products 19-22): 30% "End of Season", expired
--   Group 5 (products 23-26): 10% "New Members", active, no expiry
--   Group A: "New Season" 35%, group discount across 4 accessories
--   Group B: "Summer Bundle" 20%, cross-category group discount
-- ============================================================


-- ============================================================
-- PART 1: Mark featured products for homepage display
-- ============================================================

UPDATE listing_variants SET featured = TRUE
WHERE id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-SUIT-001',     -- 15% ERP discount
        'TPL-POLO-001',     -- 25% time-limited discount
        'TPL-BKPCK-001',    -- 10% discount
        'TPL-BOOTS-001'     -- expired discount (should show as full price)
    )
    AND lv.id IN (
        SELECT MIN(lv2.id) FROM listing_variants lv2
        WHERE lv2.product_base_id = lv.product_base_id
        GROUP BY lv2.product_base_id
    )
);


-- ============================================================
-- PART 2: Per-item discount seeds with titles and badge colours
-- ============================================================

-- Group 2: 15% "Winter Collection" (suits, blazers, sweaters…) — active, no expiry
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    15.00,
    NOW() - INTERVAL '30 days',
    NOW() + INTERVAL '365 days',
    FALSE,
    'Winter Collection',
    '#3B82F6'
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SUIT-001', 'TPL-SWEATER-001', 'TPL-CARDGN-001',
    'TPL-BLAZER-001', 'TPL-TRENCH-001', 'TPL-SHORTS-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, s.erp_item_code
FROM discounts d
JOIN skus s ON d.erp_pricing_rule_id = 'PRC-RULE-' || s.erp_item_code
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SUIT-001', 'TPL-SWEATER-001', 'TPL-CARDGN-001',
    'TPL-BLAZER-001', 'TPL-TRENCH-001', 'TPL-SHORTS-001'
)
ON CONFLICT DO NOTHING;

-- Group 3: 25% "Flash Sale" (swimwear, polo, fleece…) — active, expires in 14 days
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    25.00,
    NOW() - INTERVAL '7 days',
    NOW() + INTERVAL '14 days',
    FALSE,
    'Flash Sale',
    '#EF4444'
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SWIM-001', 'TPL-PAJAMA-001', 'TPL-POLO-001',
    'TPL-LINEN-001', 'TPL-JOGGER-001', 'TPL-FLEECE-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, s.erp_item_code
FROM discounts d
JOIN skus s ON d.erp_pricing_rule_id = 'PRC-RULE-' || s.erp_item_code
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SWIM-001', 'TPL-PAJAMA-001', 'TPL-POLO-001',
    'TPL-LINEN-001', 'TPL-JOGGER-001', 'TPL-FLEECE-001'
)
ON CONFLICT DO NOTHING;

-- Group 4: 30% "End of Season" (ties, scarves…) — expired, tests expiry filtering
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    30.00,
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '3 days',
    FALSE,
    'End of Season',
    '#6B7280'
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-TIE-001', 'TPL-SCARF-001', 'TPL-SNEAK-001', 'TPL-BOOTS-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, s.erp_item_code
FROM discounts d
JOIN skus s ON d.erp_pricing_rule_id = 'PRC-RULE-' || s.erp_item_code
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-TIE-001', 'TPL-SCARF-001', 'TPL-SNEAK-001', 'TPL-BOOTS-001'
)
ON CONFLICT DO NOTHING;

-- Group 5: 10% "New Members" (sandals, caps, bags, wallets) — active, no expiry
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
SELECT
    'PRC-RULE-' || s.erp_item_code,
    10.00,
    NOW() - INTERVAL '30 days',
    NOW() + INTERVAL '365 days',
    FALSE,
    'New Members',
    '#8B5CF6'
FROM skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SANDAL-001', 'TPL-CAP-001', 'TPL-BKPCK-001', 'TPL-WALLET-001'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, s.erp_item_code
FROM discounts d
JOIN skus s ON d.erp_pricing_rule_id = 'PRC-RULE-' || s.erp_item_code
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb ON lv.product_base_id = pb.id
WHERE pb.erp_template_code IN (
    'TPL-SANDAL-001', 'TPL-CAP-001', 'TPL-BKPCK-001', 'TPL-WALLET-001'
)
ON CONFLICT DO NOTHING;


-- ============================================================
-- PART 3: Group discounts (1 rule → many products)
-- ============================================================

-- Group A: "New Season" 35% — single rule covering 4 accessories
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
VALUES (
    'PRC-RULE-NEW-SEASON',
    35.00,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '60 days',
    FALSE,
    'New Season',
    '#F97316'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, s.erp_item_code
FROM discounts d
CROSS JOIN (
    SELECT s.erp_item_code
    FROM skus s
    JOIN listing_variants lv ON s.listing_variant_id = lv.id
    JOIN product_bases pb    ON lv.product_base_id   = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-TIE-001', 'TPL-SCARF-001', 'TPL-SNEAK-001', 'TPL-BOOTS-001'
    )
) s
WHERE d.erp_pricing_rule_id = 'PRC-RULE-NEW-SEASON'
ON CONFLICT DO NOTHING;

-- Group B: "Summer Bundle" 20% — cross-category group discount
INSERT INTO discounts (erp_pricing_rule_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
VALUES (
    'PRC-RULE-SUMMER-BUNDLE',
    20.00,
    NOW() - INTERVAL '3 days',
    NOW() + INTERVAL '30 days',
    FALSE,
    'Summer Bundle',
    '#10B981'
)
ON CONFLICT (erp_pricing_rule_id) DO NOTHING;

INSERT INTO discount_items (discount_id, item_code)
SELECT d.id, s.erp_item_code
FROM discounts d
CROSS JOIN (
    SELECT s.erp_item_code
    FROM skus s
    JOIN listing_variants lv ON s.listing_variant_id = lv.id
    JOIN product_bases pb    ON lv.product_base_id   = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-SWIM-001', 'TPL-SANDAL-001', 'TPL-CAP-001'
    )
) s
WHERE d.erp_pricing_rule_id = 'PRC-RULE-SUMMER-BUNDLE'
ON CONFLICT DO NOTHING;
