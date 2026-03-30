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
-- 1. EXTRA USERS & DELIVERED ORDERS
-- ================================================================

-- Extra test users
INSERT INTO users (phone, role, loyalty_points) VALUES
    ('+992905678901', 'USER', 150),
    ('+992906789012', 'USER', 280)
ON CONFLICT (phone) DO NOTHING;

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

-- User 2 (MANAGER): T-Shirt, Hoodie, Watch
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

-- User 5: T-Shirt, Watch
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    ((SELECT id FROM users WHERE phone = '+992905678901'), 'SO-2026-00005', 'DELIVERED', 275.00, 0, NOW() - INTERVAL '15 days', NOW() - INTERVAL '12 days');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00005'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-M'),
    'TPL-TSHIRT-001-MBK-M', 'Essential Cotton T-Shirt Midnight Black - M', 1, 25.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-TSHIRT-001-MBK-M');

INSERT INTO order_items (order_id, sku_id, sku_code, product_name, quantity, price_at_purchase)
SELECT
    (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00005'),
    (SELECT id FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M'),
    'TPL-WATCH-001-OBL-M', 'Minimalist Analog Watch Ocean Blue - M', 1, 250.00
WHERE EXISTS (SELECT 1 FROM skus WHERE sku_code = 'TPL-WATCH-001-OBL-M');

-- Extra orders for "a lot" of reviews
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (1, 'SO-2026-00007', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days'),
    (2, 'SO-2026-00008', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days'),
    (3, 'SO-2026-00009', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days'),
    (4, 'SO-2026-00010', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days'),
    (1, 'SO-2026-00011', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days'),
    (2, 'SO-2026-00012', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days'),
    (3, 'SO-2026-00013', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '11 days', NOW() - INTERVAL '10 days'),
    (4, 'SO-2026-00014', 'DELIVERED', 25.00, 0, NOW() - INTERVAL '12 days', NOW() - INTERVAL '11 days');


-- ================================================================
-- 2. APPROVED REVIEWS
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

-- Extra reviews for T-Shirt
INSERT INTO reviews (listing_variant_id, author_id, author_name, rating, body, status, order_id, created_at) VALUES
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'), 1, 'Alex M.', 5, 'Best t-shirt ever.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00007'), NOW() - INTERVAL '4 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'), 2, 'Zafar B.', 4, 'Really nice quality cotton.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00008'), NOW() - INTERVAL '5 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'), 3, 'Kamol A.', 5, 'Fits perfectly. Midnight black is a great color.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00009'), NOW() - INTERVAL '6 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'), 4, 'Dilnoza T.', 3, 'A bit too long for me.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00010'), NOW() - INTERVAL '7 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'), 4, 'Dilnoza T.', 5, 'Super soft.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00014'), NOW() - INTERVAL '11 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'), 1, 'Alex M.', 5, 'Great for gym too.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00011'), NOW() - INTERVAL '8 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black'), 2, 'Zafar B.', 4, 'Good value for money.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00012'), NOW() - INTERVAL '9 days');

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

-- Extra reviews for Watch
INSERT INTO reviews (listing_variant_id, author_id, author_name, rating, body, status, order_id, created_at) VALUES
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'), 1, 'Alex M.', 5, 'The strap is very comfortable.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00007'), NOW() - INTERVAL '4 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'), 2, 'Zafar B.', 4, 'Elegant and functional.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00008'), NOW() - INTERVAL '5 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'), (SELECT id FROM users WHERE phone = '+992905678901'), 'Elena K.', 5, 'Looks great with my suit.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00005'), NOW() - INTERVAL '12 days'),
    ((SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue'), 3, 'Kamol A.', 4, 'Very accurate movement.', 'APPROVED', (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00004'), NOW() - INTERVAL '30 days');

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


-- ================================================================
-- 3. PRODUCT QUESTIONS
-- ================================================================

-- T-Shirt questions
INSERT INTO product_questions (product_base_id, author_id, author_name, question_text, answer_text, answered_at, status, created_at) VALUES
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-TSHIRT-001'), 1, 'Alex M.', 'Does the color fade after washing?', 'No, the midnight black color is very durable and stays deep even after many washes.', NOW() - INTERVAL '10 days', 'PUBLISHED', NOW() - INTERVAL '12 days'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-TSHIRT-001'), 2, 'Zafar B.', 'Is it 100% cotton?', 'Yes, it is 100% premium pima cotton.', NOW() - INTERVAL '5 days', 'PUBLISHED', NOW() - INTERVAL '6 days'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-TSHIRT-001'), 3, 'Kamol A.', 'Can I iron it?', 'Yes, but we recommend ironing on the reverse side at medium temperature.', NOW() - INTERVAL '2 days', 'PUBLISHED', NOW() - INTERVAL '3 days'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-TSHIRT-001'), NULL, 'Anonymous', 'Is it see-through?', NULL, NULL, 'PENDING', NOW() - INTERVAL '1 day'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-TSHIRT-001'), 4, 'Dilnoza T.', 'When will more sizes be available?', 'We expect a restock of all sizes by next week.', NOW() - INTERVAL '1 hour', 'PUBLISHED', NOW() - INTERVAL '2 days');

-- Watch questions
INSERT INTO product_questions (product_base_id, author_id, author_name, question_text, answer_text, answered_at, status, created_at) VALUES
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-WATCH-001'), 1, 'Alex M.', 'Is it waterproof?', 'It is water-resistant up to 30 meters, suitable for splashes but not for swimming.', NOW() - INTERVAL '15 days', 'PUBLISHED', NOW() - INTERVAL '20 days'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-WATCH-001'), (SELECT id FROM users WHERE phone = '+992905678901'), 'Elena K.', 'Does it come with a warranty?', 'Yes, it comes with a 2-year international warranty.', NOW() - INTERVAL '8 days', 'PUBLISHED', NOW() - INTERVAL '10 days'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-WATCH-001'), (SELECT id FROM users WHERE phone = '+992906789012'), 'Mirzo S.', 'Is the strap replaceable?', 'Yes, it uses standard 20mm spring bars.', NOW() - INTERVAL '4 days', 'PUBLISHED', NOW() - INTERVAL '5 days'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-WATCH-001'), NULL, 'Shopper', 'Does the glass scratch easily?', 'It uses hardened mineral glass which is quite scratch-resistant for daily use.', NOW() - INTERVAL '1 day', 'PUBLISHED', NOW() - INTERVAL '2 days');

-- Hoodie questions
INSERT INTO product_questions (product_base_id, author_id, author_name, question_text, answer_text, answered_at, status, created_at) VALUES
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-HOODIE-001'), 2, 'Zafar B.', 'How thick is the material?', 'It is a heavyweight fleece, approximately 350 GSM.', NOW() - INTERVAL '20 days', 'PUBLISHED', NOW() - INTERVAL '25 days'),
    ((SELECT id FROM product_bases WHERE external_ref = 'TPL-HOODIE-001'), 4, 'Dilnoza T.', 'Does it shrink in the dryer?', 'We recommend air drying to prevent any potential shrinkage, although the material is pre-shrunk.', NOW() - INTERVAL '12 days', 'PUBLISHED', NOW() - INTERVAL '15 days');


-- ================================================================
-- 4. PRODUCT RATING SUMMARIES
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


-- ================================================================
-- 5. REVIEW PHOTOS (dev placeholder images via picsum.photos)
-- ================================================================

-- T-Shirt: Alex M. (5★) — 2 photos
INSERT INTO review_photos (review_id, url, sort_order)
SELECT r.id, v.url, v.sort_order
FROM reviews r
CROSS JOIN (VALUES
    ('https://picsum.photos/seed/rshirt1a/400/500', 0),
    ('https://picsum.photos/seed/rshirt1b/400/500', 1)
) AS v(url, sort_order)
WHERE r.author_id = 1
  AND r.listing_variant_id = (SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black')
  AND r.order_id = (SELECT id FROM orders WHERE external_order_id = 'SO-2025-00101');

-- T-Shirt: Dilnoza T. (4★) — 1 photo
INSERT INTO review_photos (review_id, url, sort_order)
SELECT r.id, 'https://picsum.photos/seed/rshirt2a/400/500', 0
FROM reviews r
WHERE r.author_id = 4
  AND r.listing_variant_id = (SELECT id FROM listing_variants WHERE slug = 'tpl-tshirt-001-midnight-black')
  AND r.order_id = (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002');

-- Watch: Alex M. (5★) — 3 photos
INSERT INTO review_photos (review_id, url, sort_order)
SELECT r.id, v.url, v.sort_order
FROM reviews r
CROSS JOIN (VALUES
    ('https://picsum.photos/seed/rwatch1a/400/500', 0),
    ('https://picsum.photos/seed/rwatch1b/400/500', 1),
    ('https://picsum.photos/seed/rwatch1c/400/500', 2)
) AS v(url, sort_order)
WHERE r.author_id = 1
  AND r.listing_variant_id = (SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue')
  AND r.order_id = (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00001');

-- Watch: Dilnoza T. (5★) — 1 photo
INSERT INTO review_photos (review_id, url, sort_order)
SELECT r.id, 'https://picsum.photos/seed/rwatch2a/400/500', 0
FROM reviews r
WHERE r.author_id = 4
  AND r.listing_variant_id = (SELECT id FROM listing_variants WHERE slug = 'tpl-watch-001-ocean-blue')
  AND r.order_id = (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002');

-- Hoodie: Alex M. (5★) — 2 photos
INSERT INTO review_photos (review_id, url, sort_order)
SELECT r.id, v.url, v.sort_order
FROM reviews r
CROSS JOIN (VALUES
    ('https://picsum.photos/seed/rhoodie1a/400/500', 0),
    ('https://picsum.photos/seed/rhoodie1b/400/500', 1)
) AS v(url, sort_order)
WHERE r.author_id = 1
  AND r.listing_variant_id = (SELECT id FROM listing_variants WHERE slug = 'tpl-hoodie-001-forest-green')
  AND r.order_id = (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00001');

-- Hoodie: Dilnoza T. (4★) — 1 photo
INSERT INTO review_photos (review_id, url, sort_order)
SELECT r.id, 'https://picsum.photos/seed/rhoodie2a/400/500', 0
FROM reviews r
WHERE r.author_id = 4
  AND r.listing_variant_id = (SELECT id FROM listing_variants WHERE slug = 'tpl-hoodie-001-forest-green')
  AND r.order_id = (SELECT id FROM orders WHERE external_order_id = 'SO-2026-00002');
