-- ============================================================
-- V17_1__seed_discount_scenarios.sql
--
-- DEV ONLY — Updates existing seed SKUs with varied discount
-- scenarios so all pricing display cases are testable:
--
--   Case 0: No discount      → originalPrice only
--   Case 1: ERP discount     → originalPrice + discountedPrice
--   Case 2: Loyalty only     → originalPrice (loyalty computed at runtime)
--   Case 3: ERP + Loyalty    → originalPrice + discountedPrice + loyaltyPrice
--   Case 4: Expired discount → discountedPrice set but discounted_ends_at in past
--
-- Loyalty price is NOT stored in DB — it's computed by TierPricingEnricher
-- at runtime from the user's tier. So we only seed original + discounted.
--
-- Test users from V16_1:
--   +992901234567 (USER)    → GOLD tier    (5% loyalty discount)
--   +992902345678 (MANAGER) → PLATINUM     (15% loyalty discount)
--   +992903456789 (SYSTEM)  → PLATINUM     (15% loyalty discount)
--   +992904567890 (USER)    → TITANIUM     (20% loyalty discount)
--   Anonymous               → no discount
-- ============================================================

-- ----------------------------------------------------------------
-- Group 1: Products 1-6 → CLEAR discounted_price (no ERP discount)
-- These test: Case 0 (anonymous) and Case 2 (loyalty-only for logged-in)
-- ----------------------------------------------------------------
UPDATE skus SET discounted_price = NULL, discounted_ends_at = NULL, discount_percentage = NULL
WHERE listing_variant_id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-TSHIRT-001', 'TPL-HOODIE-001', 'TPL-JEANS-001',
        'TPL-WINDBRK-001', 'TPL-SKIRT-001', 'TPL-DRESS-001'
    )
);

-- ----------------------------------------------------------------
-- Group 2: Products 7-12 → 15% ERP product discount (active, no expiry)
-- originalPrice stays, discountedPrice = originalPrice * 0.85
-- Tests: Case 1 (ERP discount) and Case 3 (ERP + loyalty stacking)
--
-- Example for $250 suit:
--   originalPrice  = 250.00
--   discountedPrice = 212.50 (15% off)
--   discountPercentage = 15.00
--   loyaltyPrice   = 201.88 (GOLD 5% off discountedPrice)
--                   = 180.63 (PLATINUM 15% off discountedPrice)
--                   = 170.00 (TITANIUM 20% off discountedPrice)
-- ----------------------------------------------------------------
UPDATE skus SET
    discounted_price = ROUND(original_price * 0.85, 2),
    discounted_ends_at = NULL,
    discount_percentage = 15.00
WHERE listing_variant_id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-SUIT-001', 'TPL-SWEATER-001', 'TPL-CARDGN-001',
        'TPL-BLAZER-001', 'TPL-TRENCH-001', 'TPL-SHORTS-001'
    )
);

-- ----------------------------------------------------------------
-- Group 3: Products 13-18 → 25% ERP discount with future expiry
-- Tests: active time-limited discount + loyalty stacking
-- ----------------------------------------------------------------
UPDATE skus SET
    discounted_price = ROUND(original_price * 0.75, 2),
    discounted_ends_at = NOW() + INTERVAL '14 days',
    discount_percentage = 25.00
WHERE listing_variant_id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-SWIM-001', 'TPL-PAJAMA-001', 'TPL-POLO-001',
        'TPL-LINEN-001', 'TPL-JOGGER-001', 'TPL-FLEECE-001'
    )
);

-- ----------------------------------------------------------------
-- Group 4: Products 19-22 → EXPIRED discount (tests expiry filtering)
-- discounted_price is set but discounted_ends_at is in the past.
-- Should behave like Case 0 (no discount) — the API must ignore these.
-- discount_percentage is set in DB but API should null it out (expired).
-- ----------------------------------------------------------------
UPDATE skus SET
    discounted_price = ROUND(original_price * 0.70, 2),
    discounted_ends_at = NOW() - INTERVAL '3 days',
    discount_percentage = 30.00
WHERE listing_variant_id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-TIE-001', 'TPL-SCARF-001', 'TPL-SNEAK-001', 'TPL-BOOTS-001'
    )
);

-- ----------------------------------------------------------------
-- Group 5: Products 23-26 → 10% ERP discount (mild, no expiry)
-- Tests: small discount + loyalty stacking
-- ----------------------------------------------------------------
UPDATE skus SET
    discounted_price = ROUND(original_price * 0.90, 2),
    discounted_ends_at = NULL,
    discount_percentage = 10.00
WHERE listing_variant_id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-SANDAL-001', 'TPL-CAP-001', 'TPL-BKPCK-001', 'TPL-WALLET-001'
    )
);

-- ----------------------------------------------------------------
-- Group 6: Products 27-30 → No ERP discount (full price)
-- Same as Group 1 — more products at full price for realism
-- ----------------------------------------------------------------
UPDATE skus SET discounted_price = NULL, discounted_ends_at = NULL, discount_percentage = NULL
WHERE listing_variant_id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.erp_template_code IN (
        'TPL-BELT-001', 'TPL-SUNGLS-001', 'TPL-WATCH-001', 'TPL-TOTE-001'
    )
);

-- ----------------------------------------------------------------
-- Mark some discounted products as featured so they show on homepage
-- ----------------------------------------------------------------
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
    -- Only first color variant of each
    AND lv.id IN (
        SELECT MIN(lv2.id) FROM listing_variants lv2
        WHERE lv2.product_base_id = lv.product_base_id
        GROUP BY lv2.product_base_id
    )
);
