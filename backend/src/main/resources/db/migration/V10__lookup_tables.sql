-- ============================================================
-- V10: Replace brittle CHECK constraints with lookup tables
--
-- VARCHAR PKs so existing columns (users.role, orders.status)
-- stay unchanged — zero JPA entity modifications needed.
-- Adding a new role or status = one INSERT, no DDL.
-- ============================================================

-- 1. Role lookup table
CREATE TABLE roles (
    name VARCHAR(16) PRIMARY KEY
);

INSERT INTO roles (name) VALUES ('USER'), ('MANAGER'), ('SYSTEM');

-- 2. Order status lookup table
CREATE TABLE order_statuses (
    name VARCHAR(32) PRIMARY KEY
);

INSERT INTO order_statuses (name) VALUES
    ('PENDING'), ('PAID'), ('SHIPPED'), ('DELIVERED'), ('CANCELLED');

-- 3. Replace the inline CHECK with a proper FK
--    Constraint name follows Postgres auto-naming: {table}_{column}_check
ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT fk_users_role
    FOREIGN KEY (role) REFERENCES roles(name);

-- 4. Add the missing constraint on orders.status (was unconstrained VARCHAR)
ALTER TABLE orders ADD CONSTRAINT fk_orders_status
    FOREIGN KEY (status) REFERENCES order_statuses(name);
