-- ================================================================
-- V4__seed_data.sql
-- Initial seed data for testing the Radolfa application.
-- Contains sample users (all roles) and products.
-- ================================================================

-- ----------------------------------------------------------------
-- USERS
-- Phone numbers are fictional Tajik numbers (+992)
-- ----------------------------------------------------------------

-- Regular user (can view products, profile, wishlist)
INSERT INTO users (phone, role) VALUES
    ('+992901234567', 'USER');

-- Manager user (can upload images, edit descriptions)
INSERT INTO users (phone, role) VALUES
    ('+992902345678', 'MANAGER');

-- System user (can sync ERP data - price/name/stock)
INSERT INTO users (phone, role) VALUES
    ('+992903456789', 'SYSTEM');

-- Additional test users
INSERT INTO users (phone, role) VALUES
    ('+992904567890', 'USER'),
    ('+992905678901', 'MANAGER');

-- ----------------------------------------------------------------
-- PRODUCTS
-- Sample products with ERP-synced data (name, price, stock)
-- and web enrichments (description, images, top_selling)
-- ----------------------------------------------------------------

INSERT INTO products (
    erp_id, name, price, stock,
    web_description, is_top_selling, images,
    last_erp_sync_at
) VALUES
-- Product 1: Laptop
(
    'ERP-LAPTOP-001',
    'MacBook Pro 14" M3',
    1999.99,
    25,
    'The most powerful MacBook Pro ever. With the M3 chip, you get groundbreaking performance and amazing battery life. Perfect for developers, designers, and creative professionals.',
    TRUE,
    ARRAY['https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800'],
    NOW()
),

-- Product 2: Smartphone
(
    'ERP-PHONE-001',
    'iPhone 15 Pro Max',
    1199.99,
    50,
    'Titanium design. A17 Pro chip. The most powerful iPhone ever with an incredible camera system featuring 5x optical zoom.',
    TRUE,
    ARRAY['https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800'],
    NOW()
),

-- Product 3: Headphones
(
    'ERP-AUDIO-001',
    'Sony WH-1000XM5',
    349.99,
    100,
    'Industry-leading noise cancellation with Auto NC Optimizer. Crystal clear hands-free calling with 4 beamforming microphones.',
    TRUE,
    ARRAY['https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800'],
    NOW()
),

-- Product 4: Monitor
(
    'ERP-DISPLAY-001',
    'LG UltraFine 27" 5K',
    1299.99,
    15,
    'Stunning 5K resolution display with P3 wide color gamut. Thunderbolt 3 connectivity for seamless Mac integration.',
    FALSE,
    ARRAY['https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800'],
    NOW()
),

-- Product 5: Keyboard
(
    'ERP-INPUT-001',
    'Keychron Q1 Pro',
    199.99,
    75,
    'Premium wireless mechanical keyboard with QMK/VIA support. Aluminum frame, hot-swappable switches, and RGB backlight.',
    FALSE,
    ARRAY['https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800'],
    NOW()
),

-- Product 6: Mouse
(
    'ERP-INPUT-002',
    'Logitech MX Master 3S',
    99.99,
    120,
    'Advanced wireless mouse with MagSpeed scrolling. Ergonomic design, quiet clicks, and customizable buttons.',
    TRUE,
    ARRAY['https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800'],
    NOW()
),

-- Product 7: Tablet
(
    'ERP-TABLET-001',
    'iPad Pro 12.9" M2',
    1099.99,
    30,
    'Supercharged by the M2 chip. Stunning Liquid Retina XDR display. Works with Apple Pencil and Magic Keyboard.',
    FALSE,
    ARRAY['https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800'],
    NOW()
),

-- Product 8: Smartwatch
(
    'ERP-WATCH-001',
    'Apple Watch Ultra 2',
    799.99,
    40,
    'The most rugged and capable Apple Watch. 49mm titanium case, precision dual-frequency GPS, and up to 36 hours of battery life.',
    FALSE,
    ARRAY['https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=800'],
    NOW()
),

-- Product 9: Camera (out of stock example)
(
    'ERP-CAMERA-001',
    'Sony A7 IV',
    2499.99,
    0,
    'Full-frame mirrorless camera with 33MP sensor. Advanced autofocus, 4K 60p video, and excellent low-light performance.',
    FALSE,
    ARRAY['https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800'],
    NOW()
),

-- Product 10: Speaker
(
    'ERP-AUDIO-002',
    'Sonos Era 300',
    449.99,
    60,
    'Premium smart speaker with spatial audio. Dolby Atmos support, WiFi and Bluetooth connectivity, and voice control.',
    FALSE,
    ARRAY['https://images.unsplash.com/photo-1545454675-3531b543be5d?w=800'],
    NOW()
);

-- ----------------------------------------------------------------
-- ERP SYNC LOG (sample entries)
-- ----------------------------------------------------------------
INSERT INTO erp_sync_log (erp_id, synced_at, status, error_message) VALUES
    ('ERP-LAPTOP-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-PHONE-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-AUDIO-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-DISPLAY-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-INPUT-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-INPUT-002', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-TABLET-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-WATCH-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-CAMERA-001', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL),
    ('ERP-AUDIO-002', NOW() - INTERVAL '1 hour', 'SUCCESS', NULL);
