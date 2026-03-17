-- ================================================================
-- V23_1__seed_variant_attributes.sql
--
-- DEV ONLY — seeds listing_variant_attributes for all 30 products
-- seeded in V6_1. Must run after V23__add_variant_attributes.sql.
--
-- Slugs are deterministic (same formula as V6_1), so we look up
-- variant IDs by slug rather than relying on V6_1's loop state.
-- ================================================================

DO $$
DECLARE
    v_codes       TEXT[] := ARRAY[
        'TPL-TSHIRT-001', 'TPL-HOODIE-001',  'TPL-JEANS-001',   'TPL-WINDBRK-001', 'TPL-SKIRT-001',
        'TPL-DRESS-001',  'TPL-SUIT-001',     'TPL-SWEATER-001', 'TPL-CARDGN-001',  'TPL-BLAZER-001',
        'TPL-TRENCH-001', 'TPL-SHORTS-001',   'TPL-SWIM-001',    'TPL-PAJAMA-001',  'TPL-POLO-001',
        'TPL-LINEN-001',  'TPL-JOGGER-001',   'TPL-FLEECE-001',  'TPL-TIE-001',     'TPL-SCARF-001',
        'TPL-SNEAK-001',  'TPL-BOOTS-001',    'TPL-SANDAL-001',  'TPL-CAP-001',     'TPL-BKPCK-001',
        'TPL-WALLET-001', 'TPL-BELT-001',     'TPL-SUNGLS-001',  'TPL-WATCH-001',   'TPL-TOTE-001'
    ];

    v_color_keys  TEXT[] := ARRAY[
        'midnight-black', 'slate-grey',   'arctic-white',
        'forest-green',   'ocean-blue',   'ruby-red',
        'desert-sand',    'lavender',     'golden-amber',
        'mint-green'
    ];

    -- Attributes per product: 'Key|Value' pairs
    v_attr_pairs  TEXT[];

    v_color_idx   INT;
    v_slug        TEXT;
    v_variant_id  BIGINT;
    v_ai          INT;

BEGIN
    FOR i IN 1..30 LOOP

        v_attr_pairs := CASE i
            -- 1: Essential Cotton T-Shirt
            WHEN 1  THEN ARRAY['Material|100% Cotton', 'Fit|Regular', 'Sleeve|Short sleeve', 'Care|Machine wash cold']
            -- 2: Premium Slim Fit Hoodie
            WHEN 2  THEN ARRAY['Material|80% Cotton, 20% Polyester', 'Fit|Slim', 'Closure|Kangaroo pocket', 'Care|Machine wash warm']
            -- 3: Stretch Denim Jeans
            WHEN 3  THEN ARRAY['Material|98% Cotton, 2% Elastane', 'Fit|Slim', 'Rise|Mid-rise', 'Care|Machine wash cold, inside out']
            -- 4: Lightweight Windbreaker
            WHEN 4  THEN ARRAY['Material|100% Nylon', 'Fit|Regular', 'Hood|Adjustable drawstring', 'Packable|Yes', 'Care|Machine wash cold']
            -- 5: Pleated Midi Skirt
            WHEN 5  THEN ARRAY['Material|100% Polyester', 'Fit|Flowy', 'Length|Midi (below knee)', 'Care|Hand wash cold']
            -- 6: Sleeveless Summer Dress
            WHEN 6  THEN ARRAY['Material|100% Rayon', 'Fit|A-line', 'Length|Midi', 'Neckline|V-neck', 'Care|Hand wash cold']
            -- 7: Tailored Business Suit
            WHEN 7  THEN ARRAY['Material|100% Wool', 'Fit|Slim', 'Lining|Full lining', 'Care|Dry clean only']
            -- 8: Organic Wool Sweater
            WHEN 8  THEN ARRAY['Material|100% Organic Wool', 'Fit|Relaxed', 'Neckline|Crew neck', 'Origin|Certified organic', 'Care|Hand wash cold']
            -- 9: Heavyweight Knit Cardigan
            WHEN 9  THEN ARRAY['Material|70% Wool, 30% Acrylic', 'Fit|Oversized', 'Closure|Open front', 'Care|Dry clean recommended']
            -- 10: Modern Cut Blazer
            WHEN 10 THEN ARRAY['Material|65% Polyester, 35% Viscose', 'Fit|Slim', 'Closure|Single-button', 'Care|Dry clean']
            -- 11: All-Season Trench Coat
            WHEN 11 THEN ARRAY['Material|65% Cotton, 35% Polyester', 'Fit|Regular', 'Belt|Removable belt included', 'Care|Dry clean']
            -- 12: Active Performance Shorts
            WHEN 12 THEN ARRAY['Material|88% Polyester, 12% Elastane', 'Fit|Athletic', 'Inseam|7 inch', 'Care|Machine wash cold']
            -- 13: Quick-Dry Swim Trunks
            WHEN 13 THEN ARRAY['Material|100% Polyester', 'Fit|Regular', 'Length|18 inch', 'Care|Rinse after use, air dry']
            -- 14: Bamboo Breathable Pajamas
            WHEN 14 THEN ARRAY['Material|95% Bamboo, 5% Elastane', 'Fit|Relaxed', 'Set|Top and bottom included', 'Care|Machine wash cold, gentle']
            -- 15: Classic Polo Shirt
            WHEN 15 THEN ARRAY['Material|100% Piqué Cotton', 'Fit|Regular', 'Collar|Polo collar', 'Care|Machine wash warm']
            -- 16: Linen Button-Down Shirt
            WHEN 16 THEN ARRAY['Material|100% Linen', 'Fit|Relaxed', 'Closure|Button-up', 'Care|Machine wash cold, low tumble dry']
            -- 17: Cargo Jogger Pants
            WHEN 17 THEN ARRAY['Material|95% Cotton, 5% Elastane', 'Fit|Tapered', 'Pockets|6 pockets including cargo', 'Care|Machine wash cold']
            -- 18: Fleece Quarter-Zip
            WHEN 18 THEN ARRAY['Material|100% Polyester Fleece', 'Fit|Regular', 'Closure|Quarter-zip', 'Care|Machine wash cold']
            -- 19: Silk Patterned Tie
            WHEN 19 THEN ARRAY['Material|100% Silk', 'Width|3.15 inches (8 cm)', 'Length|57 inches (145 cm)', 'Care|Dry clean only']
            -- 20: Soft Merino Wool Scarf
            WHEN 20 THEN ARRAY['Material|100% Merino Wool', 'Dimensions|70 × 12 inches', 'Weight|Lightweight 150g', 'Care|Hand wash cold']
            -- 21: Classic Canvas Sneakers
            WHEN 21 THEN ARRAY['Upper|100% Canvas', 'Sole|Vulcanized rubber', 'Closure|Lace-up', 'Care|Spot clean with damp cloth']
            -- 22: Rugged Waterproof Boots
            WHEN 22 THEN ARRAY['Upper|Full-grain leather', 'Sole|Rubber lug sole', 'Waterproof|Yes — seam-sealed', 'Care|Leather conditioner recommended']
            -- 23: Orthopedic Leather Sandals
            WHEN 23 THEN ARRAY['Upper|Genuine leather', 'Footbed|Memory foam insole', 'Closure|Adjustable buckle', 'Care|Wipe with damp cloth']
            -- 24: Structured Baseball Cap
            WHEN 24 THEN ARRAY['Material|100% Cotton twill', 'Fit|Adjustable back strap', 'Brim|Pre-curved, 3 inches', 'Care|Spot clean only']
            -- 25: Waterproof Laptop Backpack
            WHEN 25 THEN ARRAY['Material|600D Polyester', 'Capacity|25 litres', 'Laptop compartment|Fits up to 15.6 inch', 'Waterproof|Yes — water-resistant coating']
            -- 26: Minimalist Leather Wallet
            WHEN 26 THEN ARRAY['Material|Full-grain leather', 'Card slots|8 slots', 'Closure|Bifold', 'Dimensions|4.3 × 3.5 inches']
            -- 27: Full Grain Leather Belt
            WHEN 27 THEN ARRAY['Material|Full-grain leather', 'Width|1.5 inches (3.8 cm)', 'Buckle|Solid brass pin buckle', 'Care|Leather conditioner']
            -- 28: Vintage Aviator Sunglasses
            WHEN 28 THEN ARRAY['Frame|Stainless steel', 'Lens|Polarized UV400 protection', 'Shape|Aviator', 'Lens width|58 mm']
            -- 29: Minimalist Analog Watch
            WHEN 29 THEN ARRAY['Case|Stainless steel, 40 mm', 'Strap|Genuine leather', 'Water resistance|30 metres', 'Movement|Japanese quartz']
            -- 30: Casual Canvas Tote Bag
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

        END LOOP; -- colors
    END LOOP; -- products
END $$;
