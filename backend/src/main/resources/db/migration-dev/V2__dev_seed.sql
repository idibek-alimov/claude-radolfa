-- ================================================================
-- V2__dev_seed.sql
--
-- DEV ONLY — Realistic seed data for local development.
-- Only loaded when spring.flyway.locations includes
-- classpath:db/migration-dev (configured in application-dev.yml).
--
-- Sections:
--   1. Lookup tables (roles, order_statuses)
--   2. Loyalty tiers
--   3. Users
--   4. Categories & Colors
--   5. Product bases, listing variants, images (30 products × 3 colours)
--   6. SKUs (270 SKUs)
--   7. Variant attributes
--   8. Orders & order items
--   9. Discounts & discount items
--  10. Featured flags
-- ================================================================


-- ================================================================
-- 1. LOOKUP TABLES
-- ================================================================

-- roles and order_statuses are seeded by V1 (all environments)


-- ================================================================
-- 2. LOYALTY TIERS
-- ================================================================

INSERT INTO loyalty_tiers (name, discount_percentage, cashback_percentage, min_spend_requirement, display_order, color)
VALUES
    ('GOLD',     5.00,  2.50,  10000.00,  3, '#C8962D'),
    ('PLATINUM', 15.00, 7.50,  50000.00,  2, '#7C8EA0'),
    ('TITANIUM', 20.00, 10.00, 100000.00, 1, '#2D2D2D');


-- ================================================================
-- 3. USERS
-- ================================================================

INSERT INTO users (phone, role, loyalty_points) VALUES
    ('+992901234567', 'USER',    20),
    ('+992902345678', 'MANAGER', 30),
    ('+992903456789', 'SYNC',    50),
    ('+992904567890', 'USER',    5200);

-- User 1: Gold tier, close to Platinum
UPDATE users SET
    tier_id                = (SELECT id FROM loyalty_tiers WHERE name = 'GOLD'),
    loyalty_points         = 450,
    spend_to_next_tier     = 8000.00,
    spend_to_maintain_tier = 0.00,
    current_month_spending = 42000.00
WHERE phone = '+992901234567';

-- User 2: Platinum tier, at risk of demotion
UPDATE users SET
    tier_id                = (SELECT id FROM loyalty_tiers WHERE name = 'PLATINUM'),
    loyalty_points         = 1200,
    spend_to_next_tier     = 62000.00,
    spend_to_maintain_tier = 12000.00,
    current_month_spending = 38000.00
WHERE phone = '+992902345678';

-- User 3 (SYNC service account): Platinum tier, close to Titanium
UPDATE users SET
    tier_id                = (SELECT id FROM loyalty_tiers WHERE name = 'PLATINUM'),
    loyalty_points         = 2000,
    spend_to_next_tier     = 8000.00,
    spend_to_maintain_tier = 0.00,
    current_month_spending = 92000.00
WHERE phone = '+992903456789';

-- User 4: Titanium, top tier
UPDATE users SET
    tier_id                = (SELECT id FROM loyalty_tiers WHERE name = 'TITANIUM'),
    loyalty_points         = 5200,
    spend_to_next_tier     = NULL,
    spend_to_maintain_tier = 0.00,
    current_month_spending = 135000.00
WHERE phone = '+992904567890';


-- ================================================================
-- 4. CATEGORIES & COLORS
-- ================================================================

INSERT INTO categories (name, slug) VALUES
    ('Tops',       'tops'),
    ('Bottoms',    'bottoms'),
    ('Outerwear',  'outerwear'),
    ('Dresses',    'dresses'),
    ('Sleepwear',  'sleepwear'),
    ('Footwear',   'footwear'),
    ('Accessories','accessories');

INSERT INTO colors (color_key, display_name, hex_code) VALUES
    ('midnight-black', 'Midnight Black', '#1A1A1A'),
    ('slate-grey',     'Slate Grey',     '#708090'),
    ('arctic-white',   'Arctic White',   '#F8F8FF'),
    ('forest-green',   'Forest Green',   '#228B22'),
    ('ocean-blue',     'Ocean Blue',     '#006994'),
    ('ruby-red',       'Ruby Red',       '#9B111E'),
    ('desert-sand',    'Desert Sand',    '#C2B280'),
    ('lavender',       'Lavender',       '#B57EDC'),
    ('golden-amber',   'Golden Amber',   '#FFBF00'),
    ('mint-green',     'Mint Green',     '#98FF98');


-- ================================================================
-- 5–6. PRODUCTS, VARIANTS, IMAGES & SKUs
-- ================================================================

DO $$
DECLARE
    v_names        TEXT[] := ARRAY[
        'Essential Cotton T-Shirt',
        'Premium Slim Fit Hoodie',
        'Stretch Denim Jeans',
        'Lightweight Windbreaker',
        'Pleated Midi Skirt',
        'Sleeveless Summer Dress',
        'Tailored Business Suit',
        'Organic Wool Sweater',
        'Heavyweight Knit Cardigan',
        'Modern Cut Blazer',
        'All-Season Trench Coat',
        'Active Performance Shorts',
        'Quick-Dry Swim Trunks',
        'Bamboo Breathable Pajamas',
        'Classic Polo Shirt',
        'Linen Button-Down Shirt',
        'Cargo Jogger Pants',
        'Fleece Quarter-Zip',
        'Silk Patterned Tie',
        'Soft Merino Wool Scarf',
        'Classic Canvas Sneakers',
        'Rugged Waterproof Boots',
        'Orthopedic Leather Sandals',
        'Structured Baseball Cap',
        'Waterproof Laptop Backpack',
        'Minimalist Leather Wallet',
        'Full Grain Leather Belt',
        'Vintage Aviator Sunglasses',
        'Minimalist Analog Watch',
        'Casual Canvas Tote Bag'
    ];

    v_codes        TEXT[] := ARRAY[
        'TPL-TSHIRT-001', 'TPL-HOODIE-001',  'TPL-JEANS-001',   'TPL-WINDBRK-001', 'TPL-SKIRT-001',
        'TPL-DRESS-001',  'TPL-SUIT-001',     'TPL-SWEATER-001', 'TPL-CARDGN-001',  'TPL-BLAZER-001',
        'TPL-TRENCH-001', 'TPL-SHORTS-001',   'TPL-SWIM-001',    'TPL-PAJAMA-001',  'TPL-POLO-001',
        'TPL-LINEN-001',  'TPL-JOGGER-001',   'TPL-FLEECE-001',  'TPL-TIE-001',     'TPL-SCARF-001',
        'TPL-SNEAK-001',  'TPL-BOOTS-001',    'TPL-SANDAL-001',  'TPL-CAP-001',     'TPL-BKPCK-001',
        'TPL-WALLET-001', 'TPL-BELT-001',     'TPL-SUNGLS-001',  'TPL-WATCH-001',   'TPL-TOTE-001'
    ];

    v_categories   TEXT[] := ARRAY[
        'Tops', 'Tops', 'Bottoms', 'Outerwear', 'Bottoms',
        'Dresses', 'Outerwear', 'Tops', 'Tops', 'Outerwear',
        'Outerwear', 'Bottoms', 'Bottoms', 'Sleepwear', 'Tops',
        'Tops', 'Bottoms', 'Tops', 'Accessories', 'Accessories',
        'Footwear', 'Footwear', 'Footwear', 'Accessories', 'Accessories',
        'Accessories', 'Accessories', 'Accessories', 'Accessories', 'Accessories'
    ];

    v_prices       NUMERIC[] := ARRAY[
        25.00, 65.00, 80.00, 95.00, 55.00,
        70.00, 250.00, 110.00, 85.00, 180.00,
        200.00, 40.00, 35.00, 45.00, 50.00,
        75.00, 60.00, 55.00, 45.00, 40.00,
        70.00, 150.00, 60.00, 30.00, 90.00,
        50.00, 45.00, 120.00, 250.00, 35.00
    ];

    v_color_keys   TEXT[] := ARRAY[
        'midnight-black', 'slate-grey',   'arctic-white',
        'forest-green',   'ocean-blue',   'ruby-red',
        'desert-sand',    'lavender',     'golden-amber',
        'mint-green'
    ];

    v_color_abbrs  TEXT[] := ARRAY[
        'MBK', 'SGY', 'AWH',
        'FGN', 'OBL', 'RRD',
        'DSN', 'LAV', 'GAM',
        'MGN'
    ];

    v_clothing_sizes TEXT[] := ARRAY['S', 'M', 'L'];
    v_size_abbrs     TEXT[] := ARRAY['S', 'M', 'L'];

    v_unsplash     TEXT[] := ARRAY[
        '1583743814966-8936f5b7be1a', '1581655353564-df123a1eb820', '1521572163474-6864f9cf17ab',
        '1556821840-3a63f95609a7', '1620799140188-3b2a02fd9a77', '1564557287817-3785e38ec1f5',
        '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a',
        '1591047139829-d91aecb6caea', '1620799140188-3b2a02fd9a77', '1516762689617-e1cffcef479d',
        '1577900232427-18219b9166a0', '1583496661160-fb5886a0aaaa', '1646054224885-f978f5798312',
        '1675186049409-f9f8f60ebb5e', '1583496661160-fb5886a0aaaa', '1525507119028-ed4c629a60a3',
        '1491336477066-31156b5e4f35', '1507679799987-c73779587ccf', '1600091166971-7f9faad6c1e2',
        '1574201635302-388dd92a4c3f', '1631541909061-71e349d1f203', '1572635196237-14b3f281503f',
        '1610288311735-39b7facbd095', '1581497396202-5645e76a3a8e', '1679847628912-4c3e7402abc7',
        '1617127365659-c47fa864d8bc', '1617137984095-74e4e5e3613f', '1507679799987-c73779587ccf',
        '1591047139829-d91aecb6caea', '1516762689617-e1cffcef479d', '1572635196237-14b3f281503f',
        '1591195853828-11db59a44f6b', '1667388624717-895854eea032', '1598522325074-042db73aa4e6',
        '1591195853828-11db59a44f6b', '1667388624717-895854eea032', '1598522325074-042db73aa4e6',
        '1623832101940-647285e32a58', '1572635196237-14b3f281503f', '1516762689617-e1cffcef479d',
        '1583743814966-8936f5b7be1a', '1581655353564-df123a1eb820', '1521572163474-6864f9cf17ab',
        '1591047139829-d91aecb6caea', '1620799140188-3b2a02fd9a77', '1523381210434-271e8be1f52b',
        '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a',
        '1556821840-3a63f95609a7', '1620799140188-3b2a02fd9a77', '1564557287817-3785e38ec1f5',
        '1598033129183-c05e1ac983ff', '1591729652476-e7f587578d9c', '1589756823695-278bc923f962',
        '1457545195570-67f207084966', '1623832101940-647285e32a58', '1609803384069-19f3e5a70e75',
        '1606107557195-0e29a4b5b4aa', '1542291026-7eec264c27ff', '1525966222134-fcfa99b8ae77',
        '1550998358-08b4f83dc345', '1608256246200-53e635b5b65f', '1544441893-675973e31985',
        '1618615098938-84fc29796e76', '1603487742131-4160ec999306', '1591195853828-11db59a44f6b',
        '1588850561407-ed78c282e89b', '1691256676359-574db56b52cd', '1584917865442-de89df76afd3',
        '1553062407-98eeb64c6a62', '1509762774605-f07235a08f1f', '1598532163257-ae3c6b2524b6',
        '1614260938313-a7fc1a7ad0d2', '1579014134953-1580d7f123f3', '1627123424574-724758594e93',
        '1664286074176-5206ee5dc878', '1666723043169-22e34545675c', '1711443982852-9d22d3e4f961',
        '1577803645773-f96470509666', '1511499767150-a48a237f0083', '1572635196237-14b3f281503f',
        '1542496658-e33a6d0d50f6', '1523170335258-f5ed11844a49', '1524805444758-089113d48a6d',
        '1598532163257-ae3c6b2524b6', '1584917865442-de89df76afd3', '1591047139829-d91aecb6caea'
    ];

    v_extra_imgs   TEXT[] := ARRAY[
        '1489987707025-afc232f7ea0f', '1441984904996-e0b6ba687e04', '1525507119028-ed4c629a60a3',
        '1529720317453-c8da503f2051', '1470309864661-68328b2cd0a5', '1578681994506-b8f463449011',
        '1523381294911-8d3cead13475', '1516762689617-e1cffcef479d', '1544441893-675973e31985',
        '1523381210434-271e8be1f52b', '1591047139829-d91aecb6caea', '1620799140188-3b2a02fd9a77',
        '1564557287817-3785e38ec1f5', '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b',
        '1637069585336-827b298fe84a', '1675186049409-f9f8f60ebb5e', '1583496661160-fb5886a0aaaa',
        '1646054224885-f978f5798312', '1491336477066-31156b5e4f35', '1507679799987-c73779587ccf',
        '1600091166971-7f9faad6c1e2', '1574201635302-388dd92a4c3f', '1631541909061-71e349d1f203',
        '1610288311735-39b7facbd095', '1581497396202-5645e76a3a8e', '1617127365659-c47fa864d8bc',
        '1617137984095-74e4e5e3613f', '1667388624717-895854eea032', '1591195853828-11db59a44f6b',
        '1598522325074-042db73aa4e6', '1598033129183-c05e1ac983ff', '1591729652476-e7f587578d9c',
        '1589756823695-278bc923f962', '1457545195570-67f207084966', '1609803384069-19f3e5a70e75',
        '1606107557195-0e29a4b5b4aa', '1542291026-7eec264c27ff', '1525966222134-fcfa99b8ae77',
        '1550998358-08b4f83dc345', '1608256246200-53e635b5b65f', '1618615098938-84fc29796e76',
        '1603487742131-4160ec999306', '1588850561407-ed78c282e89b', '1553062407-98eeb64c6a62',
        '1509762774605-f07235a08f1f', '1614260938313-a7fc1a7ad0d2', '1579014134953-1580d7f123f3',
        '1664286074176-5206ee5dc878', '1577803645773-f96470509666', '1511499767150-a48a237f0083',
        '1542496658-e33a6d0d50f6', '1523170335258-f5ed11844a49', '1524805444758-089113d48a6d',
        '1598532163257-ae3c6b2524b6', '1584917865442-de89df76afd3', '1627123424574-724758594e93',
        '1666723043169-22e34545675c', '1572635196237-14b3f281503f', '1523381210434-271e8be1f52b',
        '1516762689617-e1cffcef479d', '1578681994506-b8f463449011', '1521572163474-6864f9cf17ab',
        '1556821840-3a63f95609a7', '1620799140188-3b2a02fd9a77', '1604176354204-9268737828e4',
        '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a', '1675186049409-f9f8f60ebb5e',
        '1583496661160-fb5886a0aaaa', '1646054224885-f978f5798312', '1491336477066-31156b5e4f35',
        '1507679799987-c73779587ccf', '1600091166971-7f9faad6c1e2', '1574201635302-388dd92a4c3f',
        '1631541909061-71e349d1f203', '1610288311735-39b7facbd095', '1581497396202-5645e76a3a8e',
        '1617127365659-c47fa864d8bc', '1617137984095-74e4e5e3613f', '1667388624717-895854eea032',
        '1591195853828-11db59a44f6b', '1598522325074-042db73aa4e6', '1598033129183-c05e1ac983ff',
        '1591729652476-e7f587578d9c', '1589756823695-278bc923f962', '1457545195570-67f207084966',
        '1609803384069-19f3e5a70e75', '1606107557195-0e29a4b5b4aa', '1542291026-7eec264c27ff'
    ];

    v_base_id       BIGINT;
    v_variant_id    BIGINT;
    v_color_idx     INT;
    v_si            INT;
    v_img_idx       INT;
    v_extra_offset  INT;
    v_slug          TEXT;
    v_sku_code      TEXT;
    v_base_price    NUMERIC;
    v_size_label    TEXT;
    v_stock         INT;
    v_is_top        BOOLEAN;
    v_description   TEXT;
    v_cat_id        BIGINT;
    v_color_id      BIGINT;

BEGIN
    v_extra_offset := 1;

    FOR i IN 1..30 LOOP

        SELECT id INTO v_cat_id FROM categories WHERE name = v_categories[i];

        INSERT INTO product_bases (external_ref, name, category_id, category_name)
        VALUES (v_codes[i], v_names[i], v_cat_id, v_categories[i])
        RETURNING id INTO v_base_id;

        FOR v_ci IN 1..3 LOOP
            v_color_idx := ((i - 1) * 3 + v_ci - 1) % 10 + 1;

            SELECT id INTO v_color_id FROM colors WHERE color_key = v_color_keys[v_color_idx];

            v_slug     := lower(v_codes[i]) || '-' || v_color_keys[v_color_idx];
            v_is_top   := (i IN (1,4,7,10,15,21,29) AND v_ci = 1);

            v_description := v_names[i] || ' in ' || v_color_keys[v_color_idx]
                || '. Premium quality, crafted with care for everyday comfort and style.';

            INSERT INTO listing_variants (
                product_base_id, color_id, slug, web_description, top_selling,
                product_code
            )
            VALUES (
                v_base_id, v_color_id, v_slug, v_description, v_is_top,
                'RD-' || LPAD(NEXTVAL('listing_variant_code_seq')::TEXT, 5, '0')
            )
            RETURNING id INTO v_variant_id;

            -- Primary image
            v_img_idx := (i - 1) * 3 + v_ci;
            INSERT INTO listing_variant_images (listing_variant_id, image_url, sort_order)
            VALUES (v_variant_id,
                    'https://images.unsplash.com/photo-' || v_unsplash[v_img_idx] || '?w=800&q=80',
                    0);

            -- Second image
            INSERT INTO listing_variant_images (listing_variant_id, image_url, sort_order)
            VALUES (v_variant_id,
                    'https://images.unsplash.com/photo-' || v_extra_imgs[(v_extra_offset - 1) % 90 + 1] || '?w=800&q=80',
                    1);
            v_extra_offset := v_extra_offset + 1;

            -- Third image for ~50%
            IF (i % 2 = 1) OR (v_ci = 1) THEN
                INSERT INTO listing_variant_images (listing_variant_id, image_url, sort_order)
                VALUES (v_variant_id,
                        'https://images.unsplash.com/photo-' || v_extra_imgs[(v_extra_offset - 1) % 90 + 1] || '?w=800&q=80',
                        2);
                v_extra_offset := v_extra_offset + 1;
            END IF;

            -- 3 SKUs per variant
            FOR v_si IN 1..3 LOOP
                v_size_label := v_clothing_sizes[v_si];
                -- sku_code: TEMPLATE-COLORABBR-SIZE
                v_sku_code   := v_codes[i] || '-' || v_color_abbrs[v_color_idx] || '-' || v_size_abbrs[v_si];
                v_base_price := v_prices[i] + (v_si - 1) * 5.00;

                IF ((i * 3 + v_ci + v_si) % 20 = 0) THEN
                    v_stock := 0;
                ELSE
                    v_stock := 10 + ((i * 7 + v_ci * 13 + v_si * 31) % 191);
                END IF;

                INSERT INTO skus (listing_variant_id, sku_code, size_label, stock_quantity, original_price)
                VALUES (v_variant_id, v_sku_code, v_size_label, v_stock, v_base_price);

            END LOOP; -- sizes
        END LOOP; -- colors
    END LOOP; -- products
END $$;


-- ================================================================
-- 7. VARIANT ATTRIBUTES
-- ================================================================

DO $$
DECLARE
    v_codes      TEXT[] := ARRAY[
        'TPL-TSHIRT-001', 'TPL-HOODIE-001',  'TPL-JEANS-001',   'TPL-WINDBRK-001', 'TPL-SKIRT-001',
        'TPL-DRESS-001',  'TPL-SUIT-001',     'TPL-SWEATER-001', 'TPL-CARDGN-001',  'TPL-BLAZER-001',
        'TPL-TRENCH-001', 'TPL-SHORTS-001',   'TPL-SWIM-001',    'TPL-PAJAMA-001',  'TPL-POLO-001',
        'TPL-LINEN-001',  'TPL-JOGGER-001',   'TPL-FLEECE-001',  'TPL-TIE-001',     'TPL-SCARF-001',
        'TPL-SNEAK-001',  'TPL-BOOTS-001',    'TPL-SANDAL-001',  'TPL-CAP-001',     'TPL-BKPCK-001',
        'TPL-WALLET-001', 'TPL-BELT-001',     'TPL-SUNGLS-001',  'TPL-WATCH-001',   'TPL-TOTE-001'
    ];

    v_color_keys TEXT[] := ARRAY[
        'midnight-black', 'slate-grey',   'arctic-white',
        'forest-green',   'ocean-blue',   'ruby-red',
        'desert-sand',    'lavender',     'golden-amber',
        'mint-green'
    ];

    v_attr_pairs TEXT[];
    v_color_idx  INT;
    v_slug       TEXT;
    v_variant_id BIGINT;
    v_ai         INT;

BEGIN
    FOR i IN 1..30 LOOP
        v_attr_pairs := CASE i
            WHEN 1  THEN ARRAY['Material|100% Cotton', 'Fit|Regular', 'Sleeve|Short sleeve', 'Care|Machine wash cold']
            WHEN 2  THEN ARRAY['Material|80% Cotton, 20% Polyester', 'Fit|Slim', 'Closure|Kangaroo pocket', 'Care|Machine wash warm']
            WHEN 3  THEN ARRAY['Material|98% Cotton, 2% Elastane', 'Fit|Slim', 'Rise|Mid-rise', 'Care|Machine wash cold, inside out']
            WHEN 4  THEN ARRAY['Material|100% Nylon', 'Fit|Regular', 'Hood|Adjustable drawstring', 'Packable|Yes', 'Care|Machine wash cold']
            WHEN 5  THEN ARRAY['Material|100% Polyester', 'Fit|Flowy', 'Length|Midi (below knee)', 'Care|Hand wash cold']
            WHEN 6  THEN ARRAY['Material|100% Rayon', 'Fit|A-line', 'Length|Midi', 'Neckline|V-neck', 'Care|Hand wash cold']
            WHEN 7  THEN ARRAY['Material|100% Wool', 'Fit|Slim', 'Lining|Full lining', 'Care|Dry clean only']
            WHEN 8  THEN ARRAY['Material|100% Organic Wool', 'Fit|Relaxed', 'Neckline|Crew neck', 'Origin|Certified organic', 'Care|Hand wash cold']
            WHEN 9  THEN ARRAY['Material|70% Wool, 30% Acrylic', 'Fit|Oversized', 'Closure|Open front', 'Care|Dry clean recommended']
            WHEN 10 THEN ARRAY['Material|65% Polyester, 35% Viscose', 'Fit|Slim', 'Closure|Single-button', 'Care|Dry clean']
            WHEN 11 THEN ARRAY['Material|65% Cotton, 35% Polyester', 'Fit|Regular', 'Belt|Removable belt included', 'Care|Dry clean']
            WHEN 12 THEN ARRAY['Material|88% Polyester, 12% Elastane', 'Fit|Athletic', 'Inseam|7 inch', 'Care|Machine wash cold']
            WHEN 13 THEN ARRAY['Material|100% Polyester', 'Fit|Regular', 'Length|18 inch', 'Care|Rinse after use, air dry']
            WHEN 14 THEN ARRAY['Material|95% Bamboo, 5% Elastane', 'Fit|Relaxed', 'Set|Top and bottom included', 'Care|Machine wash cold, gentle']
            WHEN 15 THEN ARRAY['Material|100% Piqué Cotton', 'Fit|Regular', 'Collar|Polo collar', 'Care|Machine wash warm']
            WHEN 16 THEN ARRAY['Material|100% Linen', 'Fit|Relaxed', 'Closure|Button-up', 'Care|Machine wash cold, low tumble dry']
            WHEN 17 THEN ARRAY['Material|95% Cotton, 5% Elastane', 'Fit|Tapered', 'Pockets|6 pockets including cargo', 'Care|Machine wash cold']
            WHEN 18 THEN ARRAY['Material|100% Polyester Fleece', 'Fit|Regular', 'Closure|Quarter-zip', 'Care|Machine wash cold']
            WHEN 19 THEN ARRAY['Material|100% Silk', 'Width|3.15 inches (8 cm)', 'Length|57 inches (145 cm)', 'Care|Dry clean only']
            WHEN 20 THEN ARRAY['Material|100% Merino Wool', 'Dimensions|70 × 12 inches', 'Weight|Lightweight 150g', 'Care|Hand wash cold']
            WHEN 21 THEN ARRAY['Upper|100% Canvas', 'Sole|Vulcanized rubber', 'Closure|Lace-up', 'Care|Spot clean with damp cloth']
            WHEN 22 THEN ARRAY['Upper|Full-grain leather', 'Sole|Rubber lug sole', 'Waterproof|Yes — seam-sealed', 'Care|Leather conditioner recommended']
            WHEN 23 THEN ARRAY['Upper|Genuine leather', 'Footbed|Memory foam insole', 'Closure|Adjustable buckle', 'Care|Wipe with damp cloth']
            WHEN 24 THEN ARRAY['Material|100% Cotton twill', 'Fit|Adjustable back strap', 'Brim|Pre-curved, 3 inches', 'Care|Spot clean only']
            WHEN 25 THEN ARRAY['Material|600D Polyester', 'Capacity|25 litres', 'Laptop compartment|Fits up to 15.6 inch', 'Waterproof|Yes — water-resistant coating']
            WHEN 26 THEN ARRAY['Material|Full-grain leather', 'Card slots|8 slots', 'Closure|Bifold', 'Dimensions|4.3 × 3.5 inches']
            WHEN 27 THEN ARRAY['Material|Full-grain leather', 'Width|1.5 inches (3.8 cm)', 'Buckle|Solid brass pin buckle', 'Care|Leather conditioner']
            WHEN 28 THEN ARRAY['Frame|Stainless steel', 'Lens|Polarized UV400 protection', 'Shape|Aviator', 'Lens width|58 mm']
            WHEN 29 THEN ARRAY['Case|Stainless steel, 40 mm', 'Strap|Genuine leather', 'Water resistance|30 metres', 'Movement|Japanese quartz']
            WHEN 30 THEN ARRAY['Material|100% Canvas', 'Capacity|20 litres', 'Handles|Reinforced double-stitched', 'Closure|Open top with inner zip pocket']
        END;

        FOR v_ci IN 1..3 LOOP
            v_color_idx := ((i - 1) * 3 + v_ci - 1) % 10 + 1;
            v_slug      := lower(v_codes[i]) || '-' || v_color_keys[v_color_idx];

            SELECT id INTO v_variant_id FROM listing_variants WHERE slug = v_slug;

            IF v_variant_id IS NULL THEN
                RAISE WARNING 'listing_variant not found for slug: %', v_slug;
                CONTINUE;
            END IF;

            FOR v_ai IN 1..array_length(v_attr_pairs, 1) LOOP
                INSERT INTO listing_variant_attributes (listing_variant_id, attr_key, attr_value, sort_order)
                VALUES (
                    v_variant_id,
                    split_part(v_attr_pairs[v_ai], '|', 1),
                    split_part(v_attr_pairs[v_ai], '|', 2),
                    v_ai - 1
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;


-- ================================================================
-- 8. ORDERS & ORDER ITEMS
-- ================================================================

-- Orders for USER (+992901234567, id=1)
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (1, 'SO-2025-00101', 'DELIVERED', 149.98, 0, NOW() - INTERVAL '30 days', NOW() - INTERVAL '25 days'),
    (1, 'SO-2025-00205', 'SHIPPED',    79.99, 0, NOW() - INTERVAL '5 days',  NOW() - INTERVAL '2 days'),
    (1, 'SO-2025-00310', 'PENDING',    34.50, 0, NOW() - INTERVAL '1 day',   NOW() - INTERVAL '1 day');

-- Orders for MANAGER (+992902345678, id=2)
INSERT INTO orders (user_id, external_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (2, 'SO-2025-00150', 'PAID',      220.00, 0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '8 days'),
    (2, 'SO-2025-00280', 'CANCELLED',  45.00, 0, NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days');

-- Order items for SO-2025-00101 (DELIVERED)
INSERT INTO order_items (order_id, sku_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE external_order_id = 'SO-2025-00101'), 'ITEM-TS-BLU-M', 'Cotton T-Shirt Blue - M', 2, 49.99),
    ((SELECT id FROM orders WHERE external_order_id = 'SO-2025-00101'), 'ITEM-TS-BLU-L', 'Cotton T-Shirt Blue - L', 1, 49.99);

-- Order items for SO-2025-00205 (SHIPPED)
INSERT INTO order_items (order_id, sku_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE external_order_id = 'SO-2025-00205'), 'ITEM-JN-BLK-32', 'Slim Jeans Black - 32', 1, 79.99);

-- Order items for SO-2025-00310 (PENDING)
INSERT INTO order_items (order_id, sku_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE external_order_id = 'SO-2025-00310'), 'ITEM-SK-WHT-S', 'Basic Socks White - Pack', 3, 11.50);

-- Order items for SO-2025-00150 (PAID)
INSERT INTO order_items (order_id, sku_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE external_order_id = 'SO-2025-00150'), 'ITEM-JK-GRN-L', 'Winter Jacket Green - L',  1, 180.00),
    ((SELECT id FROM orders WHERE external_order_id = 'SO-2025-00150'), 'ITEM-CP-GRY-M', 'Wool Cap Grey',            2,  20.00);

-- Order items for SO-2025-00280 (CANCELLED)
INSERT INTO order_items (order_id, sku_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE external_order_id = 'SO-2025-00280'), 'ITEM-BLT-BRN', 'Leather Belt Brown', 1, 45.00);


-- ================================================================
-- 9. DISCOUNTS & DISCOUNT ITEMS
-- ================================================================

-- Group 1: 15% "Winter Collection" (SEASONAL) — active, no near expiry
WITH ins AS (
    INSERT INTO discounts (discount_type_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
    VALUES (
        (SELECT id FROM discount_types WHERE name = 'SEASONAL'),
        15.00,
        NOW() - INTERVAL '30 days',
        NOW() + INTERVAL '365 days',
        FALSE,
        'Winter Collection',
        '#3B82F6'
    )
    RETURNING id
)
INSERT INTO discount_items (discount_id, item_code)
SELECT ins.id, s.sku_code
FROM ins
CROSS JOIN skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb    ON lv.product_base_id   = pb.id
WHERE pb.external_ref IN (
    'TPL-SUIT-001', 'TPL-SWEATER-001', 'TPL-CARDGN-001',
    'TPL-BLAZER-001', 'TPL-TRENCH-001', 'TPL-SHORTS-001'
)
ON CONFLICT DO NOTHING;

-- Group 2: 25% "Flash Sale" (FLASH_SALE) — active, expires in 14 days
WITH ins AS (
    INSERT INTO discounts (discount_type_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
    VALUES (
        (SELECT id FROM discount_types WHERE name = 'FLASH_SALE'),
        25.00,
        NOW() - INTERVAL '7 days',
        NOW() + INTERVAL '14 days',
        FALSE,
        'Flash Sale',
        '#EF4444'
    )
    RETURNING id
)
INSERT INTO discount_items (discount_id, item_code)
SELECT ins.id, s.sku_code
FROM ins
CROSS JOIN skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb    ON lv.product_base_id   = pb.id
WHERE pb.external_ref IN (
    'TPL-SWIM-001', 'TPL-PAJAMA-001', 'TPL-POLO-001',
    'TPL-LINEN-001', 'TPL-JOGGER-001', 'TPL-FLEECE-001'
)
ON CONFLICT DO NOTHING;

-- Group 3: 30% "End of Season" (CLEARANCE) — expired (tests expiry filtering)
WITH ins AS (
    INSERT INTO discounts (discount_type_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
    VALUES (
        (SELECT id FROM discount_types WHERE name = 'CLEARANCE'),
        30.00,
        NOW() - INTERVAL '30 days',
        NOW() - INTERVAL '3 days',
        FALSE,
        'End of Season',
        '#6B7280'
    )
    RETURNING id
)
INSERT INTO discount_items (discount_id, item_code)
SELECT ins.id, s.sku_code
FROM ins
CROSS JOIN skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb    ON lv.product_base_id   = pb.id
WHERE pb.external_ref IN (
    'TPL-TIE-001', 'TPL-SCARF-001', 'TPL-SNEAK-001', 'TPL-BOOTS-001'
)
ON CONFLICT DO NOTHING;

-- Group 4: 10% "New Members" (PROMOTIONAL) — active, no expiry
WITH ins AS (
    INSERT INTO discounts (discount_type_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
    VALUES (
        (SELECT id FROM discount_types WHERE name = 'PROMOTIONAL'),
        10.00,
        NOW() - INTERVAL '30 days',
        NOW() + INTERVAL '365 days',
        FALSE,
        'New Members',
        '#8B5CF6'
    )
    RETURNING id
)
INSERT INTO discount_items (discount_id, item_code)
SELECT ins.id, s.sku_code
FROM ins
CROSS JOIN skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb    ON lv.product_base_id   = pb.id
WHERE pb.external_ref IN (
    'TPL-SANDAL-001', 'TPL-CAP-001', 'TPL-BKPCK-001', 'TPL-WALLET-001'
)
ON CONFLICT DO NOTHING;

-- Group 5: "New Season" 35% (CLEARANCE) — single discount covering 4 accessories
WITH ins AS (
    INSERT INTO discounts (discount_type_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
    VALUES (
        (SELECT id FROM discount_types WHERE name = 'CLEARANCE'),
        35.00,
        NOW() - INTERVAL '1 day',
        NOW() + INTERVAL '60 days',
        FALSE,
        'New Season',
        '#F97316'
    )
    RETURNING id
)
INSERT INTO discount_items (discount_id, item_code)
SELECT ins.id, s.sku_code
FROM ins
CROSS JOIN skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb    ON lv.product_base_id   = pb.id
WHERE pb.external_ref IN ('TPL-TIE-001', 'TPL-SCARF-001', 'TPL-SNEAK-001', 'TPL-BOOTS-001')
ON CONFLICT DO NOTHING;

-- Group 6: "Summer Bundle" 20% (PROMOTIONAL) — cross-category
WITH ins AS (
    INSERT INTO discounts (discount_type_id, discount_value, valid_from, valid_upto, is_disabled, title, color_hex)
    VALUES (
        (SELECT id FROM discount_types WHERE name = 'PROMOTIONAL'),
        20.00,
        NOW() - INTERVAL '3 days',
        NOW() + INTERVAL '30 days',
        FALSE,
        'Summer Bundle',
        '#10B981'
    )
    RETURNING id
)
INSERT INTO discount_items (discount_id, item_code)
SELECT ins.id, s.sku_code
FROM ins
CROSS JOIN skus s
JOIN listing_variants lv ON s.listing_variant_id = lv.id
JOIN product_bases pb    ON lv.product_base_id   = pb.id
WHERE pb.external_ref IN ('TPL-SWIM-001', 'TPL-SANDAL-001', 'TPL-CAP-001')
ON CONFLICT DO NOTHING;


-- ================================================================
-- 10. FEATURED FLAGS
-- ================================================================

UPDATE listing_variants SET featured = TRUE
WHERE id IN (
    SELECT lv.id FROM listing_variants lv
    JOIN product_bases pb ON lv.product_base_id = pb.id
    WHERE pb.external_ref IN (
        'TPL-SUIT-001',
        'TPL-POLO-001',
        'TPL-BKPCK-001',
        'TPL-BOOTS-001'
    )
    AND lv.id IN (
        SELECT MIN(lv2.id) FROM listing_variants lv2
        WHERE lv2.product_base_id = lv.product_base_id
        GROUP BY lv2.product_base_id
    )
);
