-- ============================================================
-- V22_1__seed_discount_titles_and_groups.sql
--
-- DEV ONLY — Runs after V22 (one-to-many refactor).
--
-- Part 1: Patches the per-item discounts seeded in V20_1
--         with display titles and badge colours.
--
-- Part 2: Inserts two TRUE GROUP discounts — each is a single
--         Pricing Rule that spans multiple product templates,
--         demonstrating the new one-to-many capability.
-- ============================================================


-- ============================================================
-- PART 1: Give existing per-item discounts a title and colour
-- ============================================================

-- 15% "Winter Collection" (suits, blazers, sweaters…) — cool blue
UPDATE discounts
SET title = 'Winter Collection', color_hex = '#3B82F6'
WHERE discount_value = 15.00
  AND erp_pricing_rule_id LIKE 'PRC-RULE-%';

-- 25% "Flash Sale" (swimwear, polo, fleece…) — urgent red
UPDATE discounts
SET title = 'Flash Sale', color_hex = '#EF4444'
WHERE discount_value = 25.00
  AND erp_pricing_rule_id LIKE 'PRC-RULE-%';

-- 30% expired discounts (ties, scarves…) — neutral grey
-- These are expired so they will never appear on the frontend,
-- but setting the title keeps the data consistent.
UPDATE discounts
SET title = 'End of Season', color_hex = '#6B7280'
WHERE discount_value = 30.00
  AND erp_pricing_rule_id LIKE 'PRC-RULE-%';

-- 10% "New Members" (sandals, caps, bags, wallets) — violet
UPDATE discounts
SET title = 'New Members', color_hex = '#8B5CF6'
WHERE discount_value = 10.00
  AND erp_pricing_rule_id LIKE 'PRC-RULE-%';


-- ============================================================
-- PART 2: Two group discounts (1 rule → many products)
-- ============================================================

-- ---- Group A: "New Season" 35% ---- orange ----
-- Replaces the expired 30% discounts on accessories with a
-- single active pricing rule that covers all four templates.
-- Demonstrates: 1 rule_id → N item codes in discount_items.

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

-- ---- Group B: "Summer Bundle" 20% ---- emerald ----
-- One rule covering swimwear + sandals + caps — different
-- categories under a single badge, showing true cross-category
-- group discounts.

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
