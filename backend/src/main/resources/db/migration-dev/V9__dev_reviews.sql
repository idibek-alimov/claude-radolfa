-- ================================================================
-- V9__dev_reviews.sql
--
-- DEV ONLY — Seed reviews, rating summaries, and supporting orders.
-- Gives enough data to exercise every UI path:
--   - Product with multiple APPROVED reviews  → rating bars, pagination
--   - Product with a single review            → minimal summary
--   - Product with no reviews                 → section hidden
--   - PENDING review                          → not shown on storefront
--   - Seller reply                            → indented block in ReviewCard
--   - Various matchingSize values             → size fit block in RatingSummaryCard
--   - DELIVERED order for logged-in user      → "Write a Review" form enabled
-- ================================================================


-- ================================================================
-- 1. EXTRA DELIVERED ORDERS
--    (existing SO-2025-00101 for user 1 covers T-Shirt)
-- ================================================================

-- User 1: Hoodie (forest-green) + Watch (ocean-blue)
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (1, 'SO-2026-00001', 'DELIVERED', 315.00, 0, NOW() - INTERVAL '60 days', NOW() - INTERVAL '54 days');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00001'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-M'),
    'TPL-HOODIE-001-FGN-M', 'Premium Slim Fit Hoodie Forest Green - M', 1, 65.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-M');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00001'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M'),
    'TPL-WATCH-001-OBL-M', 'Minimalist Analog Watch Ocean Blue - M', 1, 250.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M');

-- User 3 (ADMIN): T-Shirt (midnight-black) + Watch (ocean-blue)
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (3, 'SO-2026-00003', 'DELIVERED', 275.00, 0, NOW() - INTERVAL '45 days', NOW() - INTERVAL '40 days');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00003'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-L'),
    'TPL-TSHIRT-001-MBK-L', 'Essential Cotton T-Shirt Midnight Black - L', 1, 25.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-L');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00003'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-S'),
    'TPL-WATCH-001-OBL-S', 'Minimalist Analog Watch Ocean Blue - S', 1, 250.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-S');

-- User 2 (MANAGER): T-Shirt, Hoodie, Watch  — used for the PENDING review + extra review coverage
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (2, 'SO-2026-00004', 'DELIVERED', 340.00, 0, NOW() - INTERVAL '35 days', NOW() - INTERVAL '30 days');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00004'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-M'),
    'TPL-TSHIRT-001-MBK-M', 'Essential Cotton T-Shirt Midnight Black - M', 1, 25.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-M');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00004'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-M'),
    'TPL-HOODIE-001-FGN-M', 'Premium Slim Fit Hoodie Forest Green - M', 1, 65.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-M');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00004'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M'),
    'TPL-WATCH-001-OBL-M', 'Minimalist Analog Watch Ocean Blue - M', 1, 250.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M');

-- User 4 (Titanium): T-Shirt, Hoodie, Jeans (desert-sand), Watch
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (4, 'SO-2026-00002', 'DELIVERED', 420.00, 0, NOW() - INTERVAL '20 days', NOW() - INTERVAL '14 days');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-S'),
    'TPL-TSHIRT-001-MBK-S', 'Essential Cotton T-Shirt Midnight Black - S', 1, 25.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-S');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-L'),
    'TPL-HOODIE-001-FGN-L', 'Premium Slim Fit Hoodie Forest Green - L', 1, 70.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-L');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-JEANS-001-DSN-M'),
    'TPL-JEANS-001-DSN-M', 'Stretch Denim Jeans Desert Sand - M', 1, 80.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-JEANS-001-DSN-M');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M'),
    'TPL-WATCH-001-OBL-M', 'Minimalist Analog Watch Ocean Blue - M', 1, 250.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M');


-- ================================================================
-- 2. APPROVED REVIEWS
--
-- Products with reviews:
--   tpl-tshirt-001-midnight-black  — 5 reviews (avg ~4.4)
--   tpl-hoodie-001-forest-green    — 3 reviews (avg ~4.0)
--   tpl-watch-001-ocean-blue       — 4 reviews (avg ~4.75)
--   tpl-jeans-001-desert-sand      — 1 review  (avg 4.0)
--   + 1 PENDING review (not shown on storefront)
-- ================================================================

-- ── T-Shirt (midnight-black) ─────────────────────────────────────

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-M'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2025-00101'),
    1, 'Alex M.', 5,
    'The perfect everyday basic',
    'I have been searching for the ideal black t-shirt for years. Soft fabric, great cut, holds its shape after multiple washes. Already ordered two more in different colours.',
    'Incredibly soft fabric, true to size, washes well',
    NULL,
    'ACCURATE',
    'APPROVED',
    NOW() - INTERVAL '22 days', NOW() - INTERVAL '20 days'
);

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, seller_reply, seller_replied_at, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-S'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    4, 'Dilnoza T.', 4,
    'Good quality, runs slightly large',
    'Nice material and clean stitching. I usually wear S but this felt more like a M. Size down if you like a fitted look.',
    'Premium cotton feel, great colour depth',
    'Runs a bit large',
    'RUNS_LARGE',
    'APPROVED',
    'Thank you for your feedback! We will update our size guide to reflect this.',
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '12 days', NOW() - INTERVAL '10 days'
);

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-L'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00003'),
    3, 'Kamol A.', 5,
    NULL,
    'Bought this on a whim and now it is my go-to. The black is a true deep black, not washed out. Highly recommended.',
    'Deep colour, durable',
    NULL,
    'ACCURATE',
    'APPROVED',
    NOW() - INTERVAL '38 days', NOW() - INTERVAL '36 days'
);

INSERT INTO reviews (
    listing_variant_id, order_id,
    author_id, author_name, rating, title, body,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00004'),
    2, 'Zafar B.', 5,
    'Bought a second time',
    'Already reviewed this before. Still five stars. My M order arrived and the fit is perfect.',
    'ACCURATE',
    'PENDING',
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
);

-- ── Hoodie (forest-green) ────────────────────────────────────────

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-hoodie-001-forest-green'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-M'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00001'),
    1, 'Alex M.', 5,
    'My new favourite hoodie',
    'Thick, warm, and the forest green is stunning. Pocket placement is perfect. Zipper feels solid. This will last years.',
    'Heavyweight fabric, excellent zipper quality',
    'None',
    'ACCURATE',
    'APPROVED',
    NOW() - INTERVAL '52 days', NOW() - INTERVAL '50 days'
);

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-hoodie-001-forest-green'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-HOODIE-001-FGN-L'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    4, 'Dilnoza T.', 4,
    'Warm and stylish',
    'Great for cold evenings. The colour photographs beautifully. I went with L as reviews suggested it runs small and it fits perfectly.',
    'Warm, good colour',
    'Slightly thin cuffs',
    'RUNS_SMALL',
    'APPROVED',
    NOW() - INTERVAL '13 days', NOW() - INTERVAL '12 days'
);

INSERT INTO reviews (
    listing_variant_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-hoodie-001-forest-green'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00004'),
    2, 'Zafar B.', 3,
    'Good but not great',
    'After a few washes the hood started to lose its shape slightly. Still comfortable but expected better durability at this price.',
    'Comfortable day-to-day',
    'Hood loses shape after washing',
    NULL,
    'APPROVED',
    NOW() - INTERVAL '30 days', NOW() - INTERVAL '28 days'
);

-- ── Watch (ocean-blue) ───────────────────────────────────────────

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00001'),
    1, 'Alex M.', 5,
    'Stunning timepiece',
    'The ocean blue dial is jaw-dropping in person. Minimal, clean, pairs with everything. Keeps accurate time. Worth every penny.',
    'Beautiful dial, accurate movement, comfortable strap',
    NULL,
    NULL,
    'APPROVED',
    NOW() - INTERVAL '53 days', NOW() - INTERVAL '51 days'
);

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    4, 'Dilnoza T.', 5,
    'Perfect gift',
    'Bought this as a gift. The packaging was premium and the watch itself looks far more expensive than it is. Very happy.',
    'Premium packaging, elegant design',
    NULL,
    NULL,
    'APPROVED',
    NOW() - INTERVAL '11 days', NOW() - INTERVAL '10 days'
);

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-S'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00003'),
    3, 'Kamol A.', 5,
    'Exceeded expectations',
    'Bought the S case for a slimmer wrist. Fits perfectly. The blue is deep and shifts shade in different lighting. Genuinely lovely watch.',
    'Versatile colour, slim case option',
    NULL,
    NULL,
    'APPROVED',
    NOW() - INTERVAL '39 days', NOW() - INTERVAL '37 days'
);

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00004'),
    2, 'Zafar B.', 4,
    NULL,
    'Solid everyday watch. Strap is slightly stiff out of the box but softens after a week of wear.',
    'Reliable, good looks',
    'Stiff strap initially',
    NULL,
    'APPROVED',
    NOW() - INTERVAL '20 days', NOW() - INTERVAL '19 days'
);

-- ── Jeans (desert-sand) ──────────────────────────────────────────

INSERT INTO reviews (
    listing_variant_id, sku_id, order_id,
    author_id, author_name, rating, title, body, pros, cons,
    matching_size, status, created_at, updated_at
) VALUES (
    (SELECT id FROM listing_variants WHERE slug = 'tpl-jeans-001-desert-sand'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-JEANS-001-DSN-M'),
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002'),
    4, 'Dilnoza T.', 4,
    'Great neutral colour',
    'Desert sand is an underrated colour for jeans. Very versatile. The stretch fabric means all-day comfort. Sizing is accurate.',
    'Comfortable stretch, great colour',
    'Back pockets slightly small',
    'ACCURATE',
    'APPROVED',
    NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days'
);


-- ================================================================
-- 3. PRODUCT RATING SUMMARIES
--    Computed from the APPROVED reviews inserted above.
-- ================================================================

INSERT INTO product_rating_summaries (
    listing_variant_id,
    average_rating,
    review_count,
    count_5, count_4, count_3, count_2, count_1,
    size_accurate, size_runs_small, size_runs_large,
    last_calculated_at
)
SELECT
    r.listing_variant_id,
    ROUND(AVG(r.rating)::NUMERIC, 2)                                          AS average_rating,
    COUNT(*)                                                                   AS review_count,
    COUNT(*) FILTER (WHERE r.rating = 5)                                       AS count_5,
    COUNT(*) FILTER (WHERE r.rating = 4)                                       AS count_4,
    COUNT(*) FILTER (WHERE r.rating = 3)                                       AS count_3,
    COUNT(*) FILTER (WHERE r.rating = 2)                                       AS count_2,
    COUNT(*) FILTER (WHERE r.rating = 1)                                       AS count_1,
    COUNT(*) FILTER (WHERE r.matching_size = 'ACCURATE')                       AS size_accurate,
    COUNT(*) FILTER (WHERE r.matching_size = 'RUNS_SMALL')                     AS size_runs_small,
    COUNT(*) FILTER (WHERE r.matching_size = 'RUNS_LARGE')                     AS size_runs_large,
    NOW()
FROM reviews r
WHERE r.status = 'APPROVED'
GROUP BY r.listing_variant_id
ON CONFLICT (listing_variant_id) DO UPDATE
    SET average_rating   = EXCLUDED.average_rating,
        review_count     = EXCLUDED.review_count,
        count_5          = EXCLUDED.count_5,
        count_4          = EXCLUDED.count_4,
        count_3          = EXCLUDED.count_3,
        count_2          = EXCLUDED.count_2,
        count_1          = EXCLUDED.count_1,
        size_accurate    = EXCLUDED.size_accurate,
        size_runs_small  = EXCLUDED.size_runs_small,
        size_runs_large  = EXCLUDED.size_runs_large,
        last_calculated_at = EXCLUDED.last_calculated_at;
