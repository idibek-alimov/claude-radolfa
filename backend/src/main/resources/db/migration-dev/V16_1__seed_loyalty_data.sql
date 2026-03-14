-- ============================================================
-- DEV ONLY: Seed loyalty tiers and update test users
-- ============================================================

-- Insert 3 loyalty tiers
INSERT INTO loyalty_tiers (name, discount_percentage, cashback_percentage, min_spend_requirement, display_order, color)
VALUES
    ('GOLD',     5.00,  2.50,  10000.00, 3, '#C8962D'),
    ('PLATINUM', 15.00, 7.50,  50000.00, 2, '#7C8EA0'),
    ('TITANIUM', 20.00, 10.00, 100000.00, 1, '#2D2D2D');

-- User 1 (+992901234567, USER): Gold tier, close to Platinum promotion
-- Spent 42,000 this month → needs 8,000 more to reach Platinum (50k)
UPDATE users SET
    tier_id = (SELECT id FROM loyalty_tiers WHERE name = 'GOLD'),
    loyalty_points = 450,
    spend_to_next_tier = 8000.00,
    spend_to_maintain_tier = 0.00,
    current_month_spending = 42000.00
WHERE phone = '+992901234567';

-- User 2 (+992902345678, MANAGER): Platinum tier, at risk of demotion
-- Spent 38,000 this month → needs 12,000 more to maintain Platinum (50k)
UPDATE users SET
    tier_id = (SELECT id FROM loyalty_tiers WHERE name = 'PLATINUM'),
    loyalty_points = 1200,
    spend_to_next_tier = 62000.00,
    spend_to_maintain_tier = 12000.00,
    current_month_spending = 38000.00
WHERE phone = '+992902345678';

-- User 3 (+992903456789, SYSTEM): Platinum tier, close to Titanium promotion
-- Spent 92,000 this month → needs 8,000 more to reach Titanium (100k)
UPDATE users SET
    tier_id = (SELECT id FROM loyalty_tiers WHERE name = 'PLATINUM'),
    loyalty_points = 2000,
    spend_to_next_tier = 8000.00,
    spend_to_maintain_tier = 0.00,
    current_month_spending = 92000.00
WHERE phone = '+992903456789';

-- User 4 (+992904567890, USER): Titanium tier — top tier, no next tier
-- Spent 135,000 this month, comfortably above 100k threshold
INSERT INTO users (phone, role, loyalty_points)
VALUES ('+992904567890', 'USER', 5200)
ON CONFLICT (phone) DO NOTHING;

UPDATE users SET
    tier_id = (SELECT id FROM loyalty_tiers WHERE name = 'TITANIUM'),
    loyalty_points = 5200,
    spend_to_next_tier = NULL,
    spend_to_maintain_tier = 0.00,
    current_month_spending = 135000.00
WHERE phone = '+992904567890';
