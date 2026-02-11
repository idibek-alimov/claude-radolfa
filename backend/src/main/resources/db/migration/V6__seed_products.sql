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
        -- T-Shirt (1)
        '1529720317453-f8e39e4e1ae4', 'xocimQVEOiE', 'QIPsRFTmYmQ',
        -- Hoodie (2)
        'FIKD9t5_5zQ', 'XKimW0pke6w', 'FGbCBiUwSPU',
        -- Jeans (3)
        'D2K1UZr4vxk', 'R3REKFP2i5E', 'Bke23QMz5TQ',
        -- Windbreaker (4)
        '0RDBOAdnbWM', 'mnZ0yEE2gOk', 'KCdYn0xu2aw',
        -- Skirt (5)
        'SIm1hOBVEKE', 'gDPaDDy6_WE', 'u3y1tB7ixrI',
        -- Dress (6)
        '_3Q3tsJ01nc', 'wFyin9rHjao', 'MOO6k3RaiwE',
        -- Suit (7)
        'ZOS1PFdBR_Q', 'WG7JmHveXCg', 'j1GiPlvSGSc',
        -- Sweater (8)
        'w2AjxfkMn84', 'Yui5vfKHuzs', 'u5nBVIfFSr0',
        -- Cardigan (9)
        'XAkPN1YEhFg', 'C1fMH2Vej8A', 'vOnwXMHSgOA',
        -- Blazer (10)
        'poI7DelFiVA', 'q4-dKLBtRjU', 'ZHvM3XIOHoE',
        -- Trench Coat (11)
        'RU6MJVxVbN4', 'WYE2UhAkif0', '8hgm6mKK04U',
        -- Shorts (12)
        'i59iwWN75KY', 'nCKcRBFDmGg', 'UYiesSO4FiM',
        -- Swim Trunks (13)
        '6te7S5nO2kA', 'VKBajRmGCrs', 'Gj5PXw99uB0',
        -- Pajamas (14)
        'xekxGBMhJDM', 'D4fipYKT3cg', 'pIGwiU0TRvY',
        -- Polo (15)
        'dP8CBD2WLVU', 'F_Pl0_vkyIU', '7BjhtdogU3A',
        -- Linen Shirt (16)
        'ZGYMfMEPxJo', 'UtwM7mDJRg4', 'b6Pme5_8nvI',
        -- Jogger (17)
        'b0g0FYHHDaY', 'gE1phX0Lbos', '2L0pFRljU9E',
        -- Fleece (18)
        'lmFJOx7hPc4', 'AvhMzHwiE_0', 'G8CBOT_pMrE',
        -- Tie (19)
        'JSlVYMtKbOA', 'FPF-p-0lnRg', 'fY8Jr4iuPQM',
        -- Scarf (20)
        'HkN64BISuQA', '2VkGObxA5jk', 'vGQ49l9I4EE',
        -- Sneakers (21)
        'TamMbr4okv4', 'kSklaGoIo30', '-mzGQvKlyQo',
        -- Boots (22)
        '5LIInaqRp5s', 'b9K3_-GPx1U', '39MVKfRJhl8',
        -- Sandals (23)
        'N1QwvJDvj4E', 'z9_GjkT1dkA', 'SazCZ97yvdA',
        -- Cap (24)
        'XZFv-0OYmhE', 'fY0lXMpB26M', 'pFyKRmDiWEA',
        -- Backpack (25)
        'tFl1hO8bLMQ', 'eDhLSGbYCVo', 'HJckKnwCXxQ',
        -- Wallet (26)
        'Y1drMFKqEhE', 'mNGaaLeWEp0', 'xEaAoizNFV8',
        -- Belt (27)
        '0ZBRKEG9Drg', 'OLlCHMbxnt0', 'Rc0r-_eaJYk',
        -- Sunglasses (28)
        'NQRZxH1Y-hg', 'UO02gAW3c0c', 'xxIcsAEk1Ks',
        -- Watch (29)
        'rKbka5sMh6I', 'MPKQiDpMyqU', 'sfL_QOnmy00',
        -- Tote Bag (30)
        'xkArbaksMKU', 'b7MZ-lFCiv0', 'bFRnKODcsbQ'
    ];

    -- Extra image IDs for 2nd/3rd images per variant
    v_extra_imgs   TEXT[] := ARRAY[
        'FnA5pAzqhMM', 'TNlHf4m4gpI', 'HH4WBGNyltc',
        'T-0EW-SEbsE', 'WNoLnJo7tS8', 'U2BI3GMnSSA',
        'qdLiIJbMVr0', 'B4TjXnI0Y2c', 'Y5bvRlcCx8k',
        'qnWPjzgQK7g', 'Xa4W7wmhdmk', 'SYTO3xs06fU',
        'XMcoTHgNcQA', 'CabUqKlCkMY', 'Aoh4jLR5M4o',
        'LTQMgx8tK-s', '0V7_N62zZcU', 'BUPbCkwqoR0',
        'U-Kty6HxcQc', '93Ep1dhTJOg', 'mXz64B-3EzM',
        'pMW4jzELQCw', 'FPjNRUaO__g', 'OOE4xAnBhKo',
        'ZelenyiVlad', 'xBFTjrMIC0c', 'l5Tzv1alcps',
        'YlOdCaABiSY', 'rp7MBx_4oXA', 'DTyjDyc8MYY',
        'xND0R8S55bM', 'b5S4FrJb7yI', '6F7hMRC4Sjw',
        'JWiMShWiF14', '5a6sy9vDTvY', '8manzosDSGM',
        'PCYw7EUzKcI', 'g9KFz-hs4kE', 'SavQfLRm4Do',
        'YUu3JCEGy-0', '2EGNqazbAMk', 'dESMzoBGJ7c',
        'hCb3lIB8L8E', 'oIpFYxtaR_Q', 'yz7MjNKqXkA',
        'MNtag_e_FbI', '1KENKTjODLk', 'dEkLm7ZJxTA',
        'r1Re6hg29hI', '4f7r1LuKu8w', 'WtwSsqwYnH0',
        'pn2aVMO0lvE', 'GtOo21gaXmQ', '8PyBbDGMm8c',
        'mJ1pB2mWd5I', '3K9WOhqP-OY', 'V-LbUqn2SDw',
        'Jp2mAfLDWBs', 'P7IuVAcnXRE', 'ehyV_EoIPWE',
        'RmoWqDCqN2E', 'DNkoNXQti3c', '3wkKGJYNJcU',
        'xHjHL-n3JJw', '_SFJhRPzJHs', 'YMOjT0PjCGo',
        'v4op_GX5fRg', '0AIi5eA_FqE', 'hpRGrfOIybc',
        '3_2dSYdExmQ', 'eDePtt6xKCE', 'EwKXn5CapA4',
        'I2yvAo2GFrc', 'L_LcE4sQDUQ', 'wTPp323zAEw',
        '6_fUMn0jSjQ', 'D3Mag4JIVRo', 'mGZX2MOPR-s',
        'OoY695oFHJg', 'G_6n5M5-caw', 'RJeGJ6JZXCI',
        'K62u25Jk6vo', 'w1JE5duY62M', 'PGnqT0rXWLs',
        '_bY_OtTcR3E', 'oHGFjHBkruA', 'K4mSJ7kQbpo',
        'Vc2dAqoSIKE', 'n5iz4rTpiLQ', 'JO19vDkXSqY'
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
