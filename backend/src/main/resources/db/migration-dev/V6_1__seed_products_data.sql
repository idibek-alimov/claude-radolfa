-- ================================================================
-- V6_1__seed_products_data.sql
--
-- DEV ONLY — 10 product templates × 3 colours × 3 sizes = 90 variants.
-- Uses the 2-layer schema: product_templates + product_variants.
-- Images stored in product_color_images (one gallery per template+color).
-- ================================================================

-- ----------------------------------------------------------------
-- Categories
-- ----------------------------------------------------------------
INSERT INTO categories (name, slug, version, created_at, updated_at) VALUES
    ('Tops',        'tops',        0, NOW(), NOW()),
    ('Bottoms',     'bottoms',     0, NOW(), NOW()),
    ('Outerwear',   'outerwear',   0, NOW(), NOW()),
    ('Dresses',     'dresses',     0, NOW(), NOW()),
    ('Footwear',    'footwear',    0, NOW(), NOW()),
    ('Accessories', 'accessories', 0, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ----------------------------------------------------------------
-- Colors lookup (for swatch display)
-- ----------------------------------------------------------------
INSERT INTO colors (color_key, display_name, hex_code, version, created_at, updated_at) VALUES
    ('Red',    'Red',    '#EF4444', 0, NOW(), NOW()),
    ('Blue',   'Blue',   '#3B82F6', 0, NOW(), NOW()),
    ('Black',  'Black',  '#111827', 0, NOW(), NOW()),
    ('White',  'White',  '#F9FAFB', 0, NOW(), NOW()),
    ('Green',  'Green',  '#10B981', 0, NOW(), NOW()),
    ('Grey',   'Grey',   '#6B7280', 0, NOW(), NOW()),
    ('Navy',   'Navy',   '#1E3A5F', 0, NOW(), NOW()),
    ('Brown',  'Brown',  '#92400E', 0, NOW(), NOW()),
    ('Pink',   'Pink',   '#EC4899', 0, NOW(), NOW()),
    ('Beige',  'Beige',  '#D4A574', 0, NOW(), NOW())
ON CONFLICT (color_key) DO NOTHING;

-- ================================================================
-- Products: 10 templates, each with 3 colors × 3 sizes = 9 variants
-- ================================================================

DO $$
DECLARE
    v_names     TEXT[] := ARRAY[
        'Essential Cotton T-Shirt',
        'Premium Slim Fit Hoodie',
        'Stretch Denim Jeans',
        'Lightweight Windbreaker',
        'Sleeveless Summer Dress',
        'Classic Polo Shirt',
        'Cargo Jogger Pants',
        'Classic Canvas Sneakers',
        'Structured Baseball Cap',
        'Minimalist Leather Wallet'
    ];
    v_codes     TEXT[] := ARRAY[
        'TPL-TSHIRT-001', 'TPL-HOODIE-001', 'TPL-JEANS-001',
        'TPL-WINDBRK-001', 'TPL-DRESS-001', 'TPL-POLO-001',
        'TPL-JOGGER-001', 'TPL-SNEAK-001', 'TPL-CAP-001', 'TPL-WALLET-001'
    ];
    v_categories TEXT[] := ARRAY[
        'Tops', 'Tops', 'Bottoms', 'Outerwear', 'Dresses',
        'Tops', 'Bottoms', 'Footwear', 'Accessories', 'Accessories'
    ];
    v_prices    NUMERIC[] := ARRAY[
        25.00, 65.00, 80.00, 95.00, 70.00,
        50.00, 60.00, 70.00, 30.00, 50.00
    ];
    v_colors    TEXT[] := ARRAY['Red', 'Blue', 'Black'];
    v_sizes     TEXT[] := ARRAY['S', 'M', 'L'];

    v_unsplash  TEXT[] := ARRAY[
        '1583743814966-8936f5b7be1a', '1581655353564-df123a1eb820', '1521572163474-6864f9cf17ab',
        '1556821840-3a63f95609a7', '1620799140188-3b2a02fd9a77', '1564557287817-3785e38ec1f5',
        '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a',
        '1591047139829-d91aecb6caea', '1516762689617-e1cffcef479d', '1523381210434-271e8be1f52b',
        '1675186049409-f9f8f60ebb5e', '1583496661160-fb5886a0aaaa', '1525507119028-ed4c629a60a3',
        '1583743814966-8936f5b7be1a', '1581655353564-df123a1eb820', '1521572163474-6864f9cf17ab',
        '1604176354204-9268737828e4', '1602293589930-3b9b6f2b453b', '1637069585336-827b298fe84a',
        '1606107557195-0e29a4b5b4aa', '1542291026-7eec264c27ff', '1525966222134-fcfa99b8ae77',
        '1588850561407-ed78c282e89b', '1691256676359-574db56b52cd', '1584917865442-de89df76afd3',
        '1614260938313-a7fc1a7ad0d2', '1579014134953-1580d7f123f3', '1627123424574-724758594e93'
    ];

    v_template_id BIGINT;
    v_cat_id      BIGINT;
    v_img_idx     INT;
    v_erp_code    TEXT;
    v_slug        TEXT;
    v_price       NUMERIC;
    v_stock       INT;
    v_attrs_def   JSONB;
    v_images      JSONB;
    v_description TEXT;
    v_is_top      BOOLEAN;
    v_is_feat     BOOLEAN;
BEGIN
    FOR i IN 1..10 LOOP
        -- Build attributes_definition for the template
        v_attrs_def := jsonb_build_object(
            'Color', jsonb_build_array('Red', 'Blue', 'Black'),
            'Size',  jsonb_build_array('S', 'M', 'L')
        );

        v_description := v_names[i] || '. Premium quality, crafted with care for everyday comfort and style.';
        v_is_top := (i IN (1, 4, 7, 10));
        v_is_feat := (i IN (2, 5, 8));

        -- Resolve category
        SELECT id INTO v_cat_id FROM categories WHERE name = v_categories[i];

        INSERT INTO product_templates (
            erp_template_code, name, description, category_id, category_name,
            attributes_definition, is_active, top_selling, featured,
            version, created_at, updated_at
        ) VALUES (
            v_codes[i], v_names[i], v_description, v_cat_id, v_categories[i],
            v_attrs_def, TRUE, v_is_top, v_is_feat,
            0, NOW(), NOW()
        )
        RETURNING id INTO v_template_id;

        -- 3 colors: insert color images once per (template, color)
        FOR ci IN 1..3 LOOP
            v_img_idx := ((i - 1) * 3 + ci);
            v_images := jsonb_build_array(
                'https://images.unsplash.com/photo-' || v_unsplash[v_img_idx] || '?w=800&q=80',
                'https://images.unsplash.com/photo-' || v_unsplash[((v_img_idx + 3) % 30) + 1] || '?w=800&q=80'
            );

            INSERT INTO product_color_images (
                template_id, color_key, images, version, created_at, updated_at
            ) VALUES (
                v_template_id, v_colors[ci], v_images, 0, NOW(), NOW()
            );

            -- 3 sizes per color
            FOR si IN 1..3 LOOP
                v_erp_code := v_codes[i] || '-' || v_colors[ci] || '-' || v_sizes[si];
                v_slug := LOWER(v_codes[i] || '-' || v_colors[ci] || '-' || v_sizes[si]);
                v_slug := REGEXP_REPLACE(v_slug, '[^a-z0-9-]', '-', 'g');
                v_slug := REGEXP_REPLACE(v_slug, '-+', '-', 'g');

                -- Price with size increment
                v_price := v_prices[i] + (si - 1) * 5.00;

                -- Stock: mostly 10-200, ~5% at 0
                IF ((i * 3 + ci + si) % 20 = 0) THEN
                    v_stock := 0;
                ELSE
                    v_stock := 10 + ((i * 7 + ci * 13 + si * 31) % 191);
                END IF;

                INSERT INTO product_variants (
                    erp_variant_code, template_id, attributes, price, stock_qty,
                    seo_slug, is_active, last_sync_at,
                    version, created_at, updated_at
                ) VALUES (
                    v_erp_code,
                    v_template_id,
                    jsonb_build_object('Color', v_colors[ci], 'Size', v_sizes[si]),
                    v_price,
                    v_stock,
                    v_slug,
                    TRUE,
                    NOW(),
                    0, NOW(), NOW()
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
