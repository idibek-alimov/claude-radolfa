-- ================================================================
-- V11__seed_orders.sql
--
-- Sample ERP-synced orders for the three test users.
-- ================================================================

-- Orders for USER (+992901234567, id=1)
INSERT INTO orders (user_id, erp_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (1, 'SO-2025-00101', 'DELIVERED', 149.98, 0, NOW() - INTERVAL '30 days', NOW() - INTERVAL '25 days'),
    (1, 'SO-2025-00205', 'SHIPPED',    79.99, 0, NOW() - INTERVAL '5 days',  NOW() - INTERVAL '2 days'),
    (1, 'SO-2025-00310', 'PENDING',    34.50, 0, NOW() - INTERVAL '1 day',   NOW() - INTERVAL '1 day');

-- Orders for MANAGER (+992902345678, id=2)
INSERT INTO orders (user_id, erp_order_id, status, total_amount, version, created_at, updated_at) VALUES
    (2, 'SO-2025-00150', 'PAID',      220.00, 0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '8 days'),
    (2, 'SO-2025-00280', 'CANCELLED',  45.00, 0, NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days');

-- Order items for SO-2025-00101 (DELIVERED)
INSERT INTO order_items (order_id, erp_item_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE erp_order_id = 'SO-2025-00101'), 'ITEM-TS-BLU-M', 'Cotton T-Shirt Blue - M', 2, 49.99),
    ((SELECT id FROM orders WHERE erp_order_id = 'SO-2025-00101'), 'ITEM-TS-BLU-L', 'Cotton T-Shirt Blue - L', 1, 49.99);

-- Order items for SO-2025-00205 (SHIPPED)
INSERT INTO order_items (order_id, erp_item_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE erp_order_id = 'SO-2025-00205'), 'ITEM-JN-BLK-32', 'Slim Jeans Black - 32', 1, 79.99);

-- Order items for SO-2025-00310 (PENDING)
INSERT INTO order_items (order_id, erp_item_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE erp_order_id = 'SO-2025-00310'), 'ITEM-SK-WHT-S', 'Basic Socks White - Pack', 3, 11.50);

-- Order items for SO-2025-00150 (PAID)
INSERT INTO order_items (order_id, erp_item_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE erp_order_id = 'SO-2025-00150'), 'ITEM-JK-GRN-L', 'Winter Jacket Green - L', 1, 180.00),
    ((SELECT id FROM orders WHERE erp_order_id = 'SO-2025-00150'), 'ITEM-CP-GRY-M', 'Wool Cap Grey', 2, 20.00);

-- Order items for SO-2025-00280 (CANCELLED)
INSERT INTO order_items (order_id, erp_item_code, product_name, quantity, price_at_purchase) VALUES
    ((SELECT id FROM orders WHERE erp_order_id = 'SO-2025-00280'), 'ITEM-BLT-BRN', 'Leather Belt Brown', 1, 45.00);
