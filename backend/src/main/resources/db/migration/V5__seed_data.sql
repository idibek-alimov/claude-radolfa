-- ================================================================
-- V5__seed_data.sql
--
-- Seeds lookup tables and test users.
-- ================================================================

-- ----------------------------------------------------------------
-- Lookup data
-- ----------------------------------------------------------------
INSERT INTO roles (name) VALUES ('USER'), ('MANAGER'), ('SYSTEM');

INSERT INTO order_statuses (name) VALUES
    ('PENDING'), ('PAID'), ('SHIPPED'), ('DELIVERED'), ('CANCELLED');

-- ----------------------------------------------------------------
-- Test users (fictional Tajik phone numbers +992)
-- ----------------------------------------------------------------
INSERT INTO users (phone, role, loyalty_points) VALUES
    ('+992901234567', 'USER', 20),
    ('+992902345678', 'MANAGER', 30),
    ('+992903456789', 'SYSTEM', 50);
