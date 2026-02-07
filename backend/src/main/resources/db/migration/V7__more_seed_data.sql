-- ================================================================
-- V7__more_seed_data.sql
-- Additional seed data to test multi-image management and search.
-- Adds 40 products across various categories.
-- ================================================================

INSERT INTO products (
    erp_id, name, price, stock,
    web_description, is_top_selling, images,
    last_erp_sync_at
) VALUES
-- TECH CATEGORY
('ERP-TECH-001', 'Ultra-Wide Curved Monitor 34"', 849.99, 12, 'Immersive 34-inch curved display with 144Hz refresh rate. Perfect for gaming and productivity.', TRUE, ARRAY['https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800', 'https://images.unsplash.com/photo-1551645120-d70bfe84c826?w=800'], NOW()),
('ERP-TECH-002', 'Mechanical Keyboard RGB', 159.50, 45, 'Satisfying tactile feedback with customizable per-key RGB lighting. Hot-swappable switches.', FALSE, ARRAY['https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?w=800', 'https://images.unsplash.com/photo-1595225476474-87563907a212?w=800'], NOW()),
('ERP-TECH-003', 'Wireless Noise Canceling Earbuds', 199.99, 80, 'Compact earbuds with industry-leading noise cancellation and 30-hour battery life.', TRUE, ARRAY['https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800', 'https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?w=800'], NOW()),
('ERP-TECH-004', 'Desktop PC Case White', 129.99, 15, 'Elegant minimalist white case with tempered glass side panel and excellent airflow.', FALSE, ARRAY['https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800', 'https://images.unsplash.com/photo-1591488320449-011701bb6704?w=800'], NOW()),
('ERP-TECH-005', 'Thunderbolt 4 Dock', 299.00, 20, 'Expand your connectivity with 12 ports including dual 4K display support.', FALSE, ARRAY['https://images.unsplash.com/photo-1563986768494-4dee2763ff3f?w=800', 'https://images.unsplash.com/photo-1544006659-f0b21f04cb1d?w=800'], NOW()),

-- HOME CATEGORY
('ERP-HOME-001', 'Ergonomic Office Chair', 399.00, 30, 'Full lumbar support with adjustable armrests and breathable mesh back.', TRUE, ARRAY['https://images.unsplash.com/photo-1505797149-35ebcb05a6fd?w=800', 'https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=800'], NOW()),
('ERP-HOME-002', 'Minimalist Desk Lamp', 59.99, 100, 'Touch-controlled LED lamp with 5 color temperatures and adjustable brightness.', FALSE, ARRAY['https://images.unsplash.com/photo-1534073828943-f801091bb18c?w=800', 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800'], NOW()),
('ERP-HOME-003', 'Standing Desk Frame', 499.00, 10, 'Heavy-duty dual motor frame with memory presets for height adjustment.', FALSE, ARRAY['https://images.unsplash.com/photo-1595515106969-1ce29566ff1c?w=800', 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800'], NOW()),
('ERP-HOME-004', 'Velvet Accent Chair', 249.00, 25, 'Mid-century modern style with soft velvet upholstery and gold legs.', TRUE, ARRAY['https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=800', 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800'], NOW()),
('ERP-HOME-005', 'Bamboo Bedside Table', 89.00, 40, 'Natural bamboo table with two drawers for organized storage.', FALSE, ARRAY['https://images.unsplash.com/photo-1532372320572-0da21c82328d?w=800', 'https://images.unsplash.com/photo-1540638349517-3abd5afc5847?w=800'], NOW()),

-- LIFESTYLE CATEGORY
('ERP-LIFE-001', 'Premium Leather Backpack', 180.00, 50, 'Handcrafted top-grain leather backpack. Fits 15-inch laptops comfortably.', TRUE, ARRAY['https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=800', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800', 'https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800'], NOW()),
('ERP-LIFE-002', 'Stainless Steel Water Bottle', 35.00, 200, 'Vacuum insulated bottle keeps drinks cold for 24 hours or hot for 12.', FALSE, ARRAY['https://images.unsplash.com/photo-1602143393494-1da210ee4a27?w=800', 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=800'], NOW()),
('ERP-LIFE-003', 'Eco-Friendly Yoga Mat', 75.00, 60, 'Non-slip natural rubber yoga mat for high-performance practice.', FALSE, ARRAY['https://images.unsplash.com/photo-1592419044706-39796d40f98c?w=800', 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800'], NOW()),
('ERP-LIFE-004', 'Noise-Masking Sleep Buds', 249.00, 30, 'Designed specifically for sleep. Masks noise so you can stay asleep.', TRUE, ARRAY['https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800', 'https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800'], NOW()),
('ERP-LIFE-005', 'Polarized Sunglasses', 120.00, 85, 'Timeless design with premium polarized lenses for superior glare reduction.', TRUE, ARRAY['https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800', 'https://images.unsplash.com/photo-1511499767350-a1590fdb44bf?w=800'], NOW()),

-- APPLIANCES CATEGORY
('ERP-APPL-001', 'Smart Coffee Maker', 229.00, 18, 'Brew from your phone. Programmable settings for the perfect morning cup.', TRUE, ARRAY['https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800', 'https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=800'], NOW()),
('ERP-APPL-002', 'High-Speed Variable Blender', 449.00, 12, 'Commercial-grade blender for smoothies, soups, and nut butters.', FALSE, ARRAY['https://images.unsplash.com/photo-1570222054627-593362668b27?w=800', 'https://images.unsplash.com/photo-1585238341267-1cf922938208?w=800'], NOW()),
('ERP-APPL-003', 'Air Fryer XL', 149.00, 55, 'Fast, healthy cooking with 90% less oil. 6-quart capacity.', TRUE, ARRAY['https://images.unsplash.com/photo-1626074353765-517a681e40be?w=800', 'https://images.unsplash.com/photo-1589733901241-5e55cd297b74?w=800'], NOW()),
('ERP-APPL-004', 'Multi-Function Toaster', 89.00, 40, '4-slice toaster with dedicated bagel and defrost settings.', FALSE, ARRAY['https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800', 'https://images.unsplash.com/photo-1522338242992-e1a54906cd8a?w=800'], NOW()),
('ERP-APPL-005', 'Robotic Vacuum Cleaner', 399.00, 22, 'Intelligent mapping and powerful suction for automated home cleaning.', TRUE, ARRAY['https://images.unsplash.com/photo-1589139011550-2b509386d117?w=800', 'https://images.unsplash.com/photo-1563212719-74e2d361c402?w=800'], NOW()),

-- GARDEN CATEGORY
('ERP-GARD-001', 'Self-Watering Planter', 45.00, 150, 'Eliminate guesswork. Keeps your plants hydrated for weeks.', FALSE, ARRAY['https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=800', 'https://images.unsplash.com/photo-1592150621344-78bb42f4ec6f?w=800'], NOW()),
('ERP-GARD-002', 'Copper Watering Can', 65.00, 40, 'Classic design with a narrow spout for precise watering.', FALSE, ARRAY['https://images.unsplash.com/photo-1585314062340-f1a5a7c9328d?w=800', 'https://images.unsplash.com/photo-1591857177580-dc82b9ac4e17?w=800'], NOW()),

-- MORE TECH
('ERP-TECH-006', 'Portable SSD 2TB', 189.00, 60, 'Wait less, do more. Ultra-fast file transfers on the go.', TRUE, ARRAY['https://images.unsplash.com/photo-1597740985671-2a8a3b80502e?w=800', 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800'], NOW()),
('ERP-TECH-007', 'Gaming Mouse Pad XXL', 45.00, 120, 'Vast surface area for low-sens gaming. Water-resistant coating.', FALSE, ARRAY['https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=800', 'https://images.unsplash.com/photo-1527814475100-26213c328310?w=800'], NOW()),

-- KITCHEN
('ERP-KIT-001', 'Cast Iron Skillet 12"', 75.00, 35, 'Seasoned and ready to use. Exceptional heat retention.', TRUE, ARRAY['https://images.unsplash.com/photo-1594833211516-17b1659a8508?w=800', 'https://images.unsplash.com/photo-1593036712227-1951d1e6df9c?w=800'], NOW()),
('ERP-KIT-002', 'Professional Chef Knife', 149.00, 20, 'Forged high-carbon steel for lasting sharpness and balance.', TRUE, ARRAY['https://images.unsplash.com/photo-1593630732362-58843c30bc7b?w=800', 'https://images.unsplash.com/photo-1550989460-0adf9ea622e2?w=800'], NOW()),

-- FASHION
('ERP-FASH-001', 'Organic Cotton T-Shirt', 25.00, 500, 'Sustainable fashion. Soft, breathable, and ethically made.', FALSE, ARRAY['https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800', 'https://images.unsplash.com/photo-1562157873-818bc0726f68?w=800'], NOW()),
('ERP-FASH-002', 'Denim Jacket Classic', 95.00, 45, 'The perfect layer. Heavyweight denim with a tailored fit.', FALSE, ARRAY['https://images.unsplash.com/photo-1551537482-f2075a1d41f2?w=800', 'https://images.unsplash.com/photo-1576905341935-4011b0ea156a?w=800'], NOW()),

-- MORE LIFE
('ERP-LIFE-006', 'Essential Oil Diffuser', 49.00, 90, 'Ultrasonic technology. Large 500ml tank for all-day use.', FALSE, ARRAY['https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=800', 'https://images.unsplash.com/photo-1595981267035-7b04ca84a82d?w=800'], NOW()),
('ERP-LIFE-007', 'Hard Shell Suitcase', 299.00, 15, 'Unbreakable shell with 360-degree silent spinner wheels.', TRUE, ARRAY['https://images.unsplash.com/photo-1565026057447-bc90a3dceb87?w=800', 'https://images.unsplash.com/photo-1581553674786-636ad0e27b40?w=800'], NOW()),

-- MISC
('ERP-MISC-001', 'Bluetooth Tracker Tag', 29.00, 300, 'Find your keys with ease. Replaceable battery lasts one year.', TRUE, ARRAY['https://images.unsplash.com/photo-1610484826967-09c5720778c7?w=800', 'https://images.unsplash.com/photo-1558239027-d973702418a0?w=800'], NOW()),
('ERP-MISC-002', 'Desk Cable Organizer', 15.00, 400, 'Magnetic cable clips for a clutter-free workspace.', FALSE, ARRAY['https://images.unsplash.com/photo-1629470948467-319985292a8b?w=800', 'https://images.unsplash.com/photo-1512446733611-9099a8705007?w=800'], NOW()),
('ERP-MISC-003', 'Portable Power Bank 20k', 69.00, 150, 'High-capacity charging for your smartphone on long trips.', TRUE, ARRAY['https://images.unsplash.com/photo-1609592424109-dd03544bccb7?w=800', 'https://images.unsplash.com/photo-1588508065123-287b28e013da?w=800'], NOW()),
('ERP-MISC-004', 'Screen Cleaning Kit', 19.99, 250, 'Alcohol-free formula for safe and streak-free cleaning.', FALSE, ARRAY['https://images.unsplash.com/photo-1585338107529-13afc5f02586?w=800', 'https://images.unsplash.com/photo-1585338447937-7082f89763d5?w=800'], NOW()),
('ERP-MISC-005', 'Compact Umbrella', 35.00, 180, 'Wind-tunnel tested. Tucks easily into bags.', FALSE, ARRAY['https://images.unsplash.com/photo-1521124618218-e39f37989341?w=800', 'https://images.unsplash.com/photo-1533659828870-95ee305ceead?w=800'], NOW()),
('ERP-MISC-006', 'Pocket Notebook Set', 25.00, 350, 'Pack of 3. Dotted grid pages for versatile note-taking.', FALSE, ARRAY['https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=800', 'https://images.unsplash.com/photo-1516414912300-8d6d87f0ce20?w=800'], NOW()),
('ERP-MISC-007', 'Brass Ballpoint Pen', 85.00, 40, 'Balanced weight for smooth writing. Develops a unique patina.', TRUE, ARRAY['https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?w=800', 'https://images.unsplash.com/photo-1566737236500-c8ac4dc016a3?w=800'], NOW()),
('ERP-MISC-008', 'Minimalist Wallet', 55.00, 110, 'Aluminum cardholder with RFID protection.', TRUE, ARRAY['https://images.unsplash.com/photo-1627123424574-724758594e93?w=800', 'https://images.unsplash.com/photo-1522071823931-0d7e7936a44d?w=800'], NOW()),
('ERP-MISC-009', 'Leather Key Fob', 20.00, 150, 'Vegetable-tanned leather with a heavy-duty brass split ring.', FALSE, ARRAY['https://images.unsplash.com/photo-1569012871812-f38ee64cd54c?w=800', 'https://images.unsplash.com/photo-1535313392151-17ca44199824?w=800'], NOW()),
('ERP-MISC-010', 'Desk Mat Felt', 45.00, 80, 'Large natural wool felt mat to protect your desk surface.', FALSE, ARRAY['https://images.unsplash.com/photo-1616627188218-d0bd2229398a?w=800', 'https://images.unsplash.com/photo-1616423640778-28d1b1332fdb?w=800'], NOW());
