-- ================================================================
-- V6__seed_products.sql
--
-- Seeds 30 product bases with 3 colour variants each (90 variants),
-- 2-3 images per variant (~225 images), and 3 size SKUs per variant
-- (270 SKUs) for frontend development.
-- ================================================================

DO $$
DECLARE
    -- Product definition arrays (parallel arrays, index 1..30)
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
        'TPL-TSHIRT-001',
        'TPL-HOODIE-001',
        'TPL-JEANS-001',
        'TPL-WINDBRK-001',
        'TPL-SKIRT-001',
        'TPL-DRESS-001',
        'TPL-SUIT-001',
        'TPL-SWEATER-001',
        'TPL-CARDGN-001',
        'TPL-BLAZER-001',
        'TPL-TRENCH-001',
        'TPL-SHORTS-001',
        'TPL-SWIM-001',
        'TPL-PAJAMA-001',
        'TPL-POLO-001',
        'TPL-LINEN-001',
        'TPL-JOGGER-001',
        'TPL-FLEECE-001',
        'TPL-TIE-001',
        'TPL-SCARF-001',
        'TPL-SNEAK-001',
        'TPL-BOOTS-001',
        'TPL-SANDAL-001',
        'TPL-CAP-001',
        'TPL-BKPCK-001',
        'TPL-WALLET-001',
        'TPL-BELT-001',
        'TPL-SUNGLS-001',
        'TPL-WATCH-001',
        'TPL-TOTE-001'
    ];

    -- Base prices per product (USD)
    v_prices       NUMERIC[] := ARRAY[
        25.00, 65.00, 80.00, 95.00, 55.00,
        70.00, 250.00, 110.00, 85.00, 180.00,
        200.00, 40.00, 35.00, 45.00, 50.00,
        75.00, 60.00, 55.00, 45.00, 40.00,
        70.00, 150.00, 60.00, 30.00, 90.00,
        50.00, 45.00, 120.00, 250.00, 35.00
    ];

    -- Color pool: name, key (for slug), abbreviation (for ERP code)
    v_color_names  TEXT[] := ARRAY[
        'Midnight Black', 'Slate Grey',   'Arctic White',
        'Forest Green',   'Ocean Blue',   'Ruby Red',
        'Desert Sand',    'Lavender',     'Golden Amber',
        'Mint Green'
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

    -- Each product uses 3 colors; define starting index into color pool (1-based, wraps)
    -- Product i uses colors at offset (i*3-2), (i*3-1), (i*3) mod 10
    v_color_idx    INT;

    -- Size labels (clothing gets S/M/L, accessories get their own)
    v_clothing_sizes TEXT[] := ARRAY['S', 'M', 'L'];
    v_size_abbrs     TEXT[] := ARRAY['S', 'M', 'L'];

    -- Unsplash photo IDs — 90 unique IDs (3 per product base × 30), used as primary images
    -- Each variant gets 2-3 images using nearby IDs
    v_unsplash     TEXT[] := ARRAY[
        -- Product 1-30 (3 images each = 90 total)
        -- Product 1 (T-Shirt)
        '1583743814966-8936f5b7be1a', '1581655353564-df123a1eb820', '1521572163474-6864f9cf17ab',
        -- Product 2 (Hoodie)
        '1556821840-3a63f95609a7', '1620799140188-3b2a02fd9a77', '1564557287817-3785e38ec1f5',
        -- Product 3 (Jeans)
        '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a',
        -- Product 4 (Windbreaker)
        '1591047139829-d91aecb6caea', '1620799140188-3b2a02fd9a77', '1516762689617-e1cffcef479d',
        -- Product 5 (Skirt)
        '1577900232427-18219b9166a0', '1583496661160-fb5886a0aaaa', '1646054224885-f978f5798312',
        -- Product 6 (Dress)
        '1675186049409-f9f8f60ebb5e', '1583496661160-fb5886a0aaaa', '1525507119028-ed4c629a60a3',
        -- Product 7 (Suit)
        '1491336477066-31156b5e4f35', '1507679799987-c73779587ccf', '1600091166971-7f9faad6c1e2',
        -- Product 8 (Sweater)
        '1574201635302-388dd92a4c3f', '1631541909061-71e349d1f203', '1572635196237-14b3f281503f',
        -- Product 9 (Cardigan)
        '1610288311735-39b7facbd095', '1581497396202-5645e76a3a8e', '1679847628912-4c3e7402abc7',
        -- Product 10 (Blazer)
        '1617127365659-c47fa864d8bc', '1617137984095-74e4e5e3613f', '1507679799987-c73779587ccf',
        -- Product 11 (Trench Coat)
        '1591047139829-d91aecb6caea', '1516762689617-e1cffcef479d', '1572635196237-14b3f281503f',
        -- Product 12 (Shorts)
        '1591195853828-11db59a44f6b', '1667388624717-895854eea032', '1598522325074-042db73aa4e6',
        -- Product 13 (Swim Trunks)
        '1591195853828-11db59a44f6b', '1667388624717-895854eea032', '1598522325074-042db73aa4e6',
        -- Product 14 (Pajamas)
        '1623832101940-647285e32a58', '1572635196237-14b3f281503f', '1516762689617-e1cffcef479d',
        -- Product 15 (Polo)
        '1583743814966-8936f5b7be1a', '1581655353564-df123a1eb820', '1521572163474-6864f9cf17ab',
        -- Product 16 (Linen Shirt)
        '1591047139829-d91aecb6caea', '1620799140188-3b2a02fd9a77', '1523381210434-271e8be1f52b',
        -- Product 17 (Jogger)
        '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a',
        -- Product 18 (Fleece)
        '1556821840-3a63f95609a7', '1620799140188-3b2a02fd9a77', '1564557287817-3785e38ec1f5',
        -- Product 19 (Tie)
        '1598033129183-c05e1ac983ff', '1591729652476-e7f587578d9c', '1589756823695-278bc923f962',
        -- Product 20 (Scarf)
        '1457545195570-67f207084966', '1623832101940-647285e32a58', '1609803384069-19f3e5a70e75',
        -- Product 21 (Sneakers)
        '1606107557195-0e29a4b5b4aa', '1542291026-7eec264c27ff', '1525966222134-fcfa99b8ae77',
        -- Product 22 (Boots)
        '1550998358-08b4f83dc345', '1608256246200-53e635b5b65f', '1544441893-675973e31985',
        -- Product 23 (Sandals)
        '1618615098938-84fc29796e76', '1603487742131-4160ec999306', '1591195853828-11db59a44f6b',
        -- Product 24 (Cap)
        '1588850561407-ed78c282e89b', '1691256676359-574db56b52cd', '1584917865442-de89df76afd3',
        -- Product 25 (Backpack)
        '1553062407-98eeb64c6a62', '1509762774605-f07235a08f1f', '1598532163257-ae3c6b2524b6',
        -- Product 26 (Wallet)
        '1614260938313-a7fc1a7ad0d2', '1579014134953-1580d7f123f3', '1627123424574-724758594e93',
        -- Product 27 (Belt)
        '1664286074176-5206ee5dc878', '1666723043169-22e34545675c', '1711443982852-9d22d3e4f961',
        -- Product 28 (Sunglasses)
        '1577803645773-f96470509666', '1511499767150-a48a237f0083', '1572635196237-14b3f281503f',
        -- Product 29 (Watch)
        '1542496658-e33a6d0d50f6', '1523170335258-f5ed11844a49', '1524805444758-089113d48a6d',
        -- Product 30 (Tote Bag)
        '1598532163257-ae3c6b2524b6', '1584917865442-de89df76afd3', '1591047139829-d91aecb6caea'
    ];

    -- Extra image IDs for 2nd/3rd images per variant
    v_extra_imgs   TEXT[] := ARRAY[
        '1489987707025-afc232f7ea0f', '1441984904996-e0b6ba687e04', '1525507119028-ed4c629a60a3', '1529720317453-c8da503f2051', '1470309864661-68328b2cd0a5', '1578681994506-b8f463449011',
        '1523381294911-8d3cead13475', '1516762689617-e1cffcef479d', '1544441893-675973e31985', '1523381210434-271e8be1f52b', '1591047139829-d91aecb6caea', '1620799140188-3b2a02fd9a77',
        '1564557287817-3785e38ec1f5', '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a', '1675186049409-f9f8f60ebb5e', '1583496661160-fb5886a0aaaa',
        '1646054224885-f978f5798312', '1491336477066-31156b5e4f35', '1507679799987-c73779587ccf', '1600091166971-7f9faad6c1e2', '1574201635302-388dd92a4c3f', '1631541909061-71e349d1f203',
        '1610288311735-39b7facbd095', '1581497396202-5645e76a3a8e', '1617127365659-c47fa864d8bc', '1617137984095-74e4e5e3613f', '1667388624717-895854eea032', '1591195853828-11db59a44f6b',
        '1598522325074-042db73aa4e6', '1598033129183-c05e1ac983ff', '1591729652476-e7f587578d9c', '1589756823695-278bc923f962', '1457545195570-67f207084966', '1609803384069-19f3e5a70e75',
        '1606107557195-0e29a4b5b4aa', '1542291026-7eec264c27ff', '1525966222134-fcfa99b8ae77', '1550998358-08b4f83dc345', '1608256246200-53e635b5b65f', '1618615098938-84fc29796e76',
        '1603487742131-4160ec999306', '1588850561407-ed78c282e89b', '1553062407-98eeb64c6a62', '1509762774605-f07235a08f1f', '1614260938313-a7fc1a7ad0d2', '1579014134953-1580d7f123f3',
        '1664286074176-5206ee5dc878', '1577803645773-f96470509666', '1511499767150-a48a237f0083', '1542496658-e33a6d0d50f6', '1523170335258-f5ed11844a49', '1524805444758-089113d48a6d',
        '1598532163257-ae3c6b2524b6', '1584917865442-de89df76afd3', '1627123424574-724758594e93', '1666723043169-22e34545675c', '1572635196237-14b3f281503f', '1523381210434-271e8be1f52b',
        '1516762689617-e1cffcef479d', '1578681994506-b8f463449011', '1521572163474-6864f9cf17ab', '1556821840-3a63f95609a7', '1620799140188-3b2a02fd9a77', '1604176354204-9268737828e4',
        '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a', '1675186049409-f9f8f60ebb5e', '1583496661160-fb5886a0aaaa', '1646054224885-f978f5798312', '1491336477066-31156b5e4f35',
        '1507679799987-c73779587ccf', '1600091166971-7f9faad6c1e2', '1574201635302-388dd92a4c3f', '1631541909061-71e349d1f203', '1610288311735-39b7facbd095', '1581497396202-5645e76a3a8e',
        '1617127365659-c47fa864d8bc', '1617137984095-74e4e5e3613f', '1667388624717-895854eea032', '1591195853828-11db59a44f6b', '1598522325074-042db73aa4e6', '1598033129183-c05e1ac983ff',
        '1591729652476-e7f587578d9c', '1589756823695-278bc923f962', '1457545195570-67f207084966', '1609803384069-19f3e5a70e75', '1606107557195-0e29a4b5b4aa', '1542291026-7eec264c27ff'
    ];

    -- Loop variables
    v_base_id      BIGINT;
    v_variant_id   BIGINT;
    v_ci           INT;   -- color iteration (1..3)
    v_si           INT;   -- size iteration (1..3)
    v_img_idx      INT;   -- index into unsplash arrays
    v_extra_offset INT;   -- offset into extra images
    v_slug         TEXT;
    v_erp_code     TEXT;
    v_base_price   NUMERIC;
    v_size_label   TEXT;
    v_stock        INT;
    v_has_sale     BOOLEAN;
    v_sale_price   NUMERIC;
    v_is_top       BOOLEAN;
    v_description  TEXT;
    v_img_count    INT;   -- 2 or 3 images per variant

BEGIN
    v_extra_offset := 1;

    FOR i IN 1..30 LOOP
        -- ============================================================
        -- Insert product_base
        -- ============================================================
        INSERT INTO product_bases (erp_template_code, name)
        VALUES (v_codes[i], v_names[i])
        RETURNING id INTO v_base_id;

        -- ============================================================
        -- 3 colour variants per base
        -- ============================================================
        FOR v_ci IN 1..3 LOOP
            -- Pick a color: product index × 3 gives a rotating window across the 10 colors
            v_color_idx := ((i - 1) * 3 + v_ci - 1) % 10 + 1;

            -- Build slug: lowercase template code + color key
            v_slug := lower(v_codes[i]) || '-' || v_color_keys[v_color_idx];

            -- ~15% top selling (products 1,4,7,10,15,21,29 — variant 1 only)
            v_is_top := (i IN (1,4,7,10,15,21,29) AND v_ci = 1);

            -- Generate a description
            v_description := v_names[i] || ' in ' || v_color_names[v_color_idx]
                || '. Premium quality, crafted with care for everyday comfort and style.';

            INSERT INTO listing_variants (
                product_base_id, color_key, slug, web_description, top_selling
            )
            VALUES (
                v_base_id,
                v_color_keys[v_color_idx],
                v_slug,
                v_description,
                v_is_top
            )
            RETURNING id INTO v_variant_id;

            -- ========================================================
            -- Images (2-3 per variant)
            -- ========================================================
            -- Primary image from main array
            v_img_idx := (i - 1) * 3 + v_ci;

            INSERT INTO listing_variant_images (listing_variant_id, image_url, sort_order)
            VALUES (
                v_variant_id,
                'https://images.unsplash.com/photo-' || v_unsplash[v_img_idx] || '?w=800&q=80',
                0
            );

            -- Second image from extra array (wraps around 90 entries)
            INSERT INTO listing_variant_images (listing_variant_id, image_url, sort_order)
            VALUES (
                v_variant_id,
                'https://images.unsplash.com/photo-' || v_extra_imgs[(v_extra_offset - 1) % 90 + 1] || '?w=800&q=80',
                1
            );
            v_extra_offset := v_extra_offset + 1;

            -- Third image for ~50% of variants (odd product index or first color)
            v_img_count := 2;
            IF (i % 2 = 1) OR (v_ci = 1) THEN
                INSERT INTO listing_variant_images (listing_variant_id, image_url, sort_order)
                VALUES (
                    v_variant_id,
                    'https://images.unsplash.com/photo-' || v_extra_imgs[(v_extra_offset - 1) % 90 + 1] || '?w=800&q=80',
                    2
                );
                v_extra_offset := v_extra_offset + 1;
                v_img_count := 3;
            END IF;

            -- ========================================================
            -- 3 SKUs per variant (sizes S, M, L)
            -- ========================================================
            -- ~20% of variants get a sale price (every 5th variant)
            v_has_sale := ((i - 1) * 3 + v_ci) % 5 = 0;

            FOR v_si IN 1..3 LOOP
                v_size_label := v_clothing_sizes[v_si];

                -- ERP item code: TEMPLATE-COLORABBR-SIZE
                v_erp_code := v_codes[i] || '-' || v_color_abbrs[v_color_idx] || '-' || v_size_abbrs[v_si];

                -- Price with size-based increment (+$5 per size step)
                v_base_price := v_prices[i] + (v_si - 1) * 5.00;

                -- Sale price: 15-25% off
                IF v_has_sale THEN
                    v_sale_price := ROUND(v_base_price * 0.80, 2);
                ELSE
                    v_sale_price := NULL;
                END IF;

                -- Stock: mostly 10-200, ~5% at 0
                IF ((i * 3 + v_ci + v_si) % 20 = 0) THEN
                    v_stock := 0;
                ELSE
                    v_stock := 10 + ((i * 7 + v_ci * 13 + v_si * 31) % 191);
                END IF;

                INSERT INTO skus (
                    listing_variant_id, erp_item_code, size_label,
                    stock_quantity, price, sale_price, sale_ends_at
                )
                VALUES (
                    v_variant_id,
                    v_erp_code,
                    v_size_label,
                    v_stock,
                    v_base_price,
                    v_sale_price,
                    CASE WHEN v_sale_price IS NOT NULL
                         THEN NOW() + INTERVAL '30 days'
                         ELSE NULL
                    END
                );
            END LOOP; -- sizes

        END LOOP; -- colors

    END LOOP; -- products

END $$;
