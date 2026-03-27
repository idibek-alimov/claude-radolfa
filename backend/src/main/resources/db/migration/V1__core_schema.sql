-- ================================================================
-- V1__core_schema.sql
--
-- Core lookups, users, and catalog reference tables.
--
-- Tables created here:
--   roles, order_statuses
--   loyalty_tiers
--   users
--   categories
--   colors
--   brands
-- ================================================================

-- ----------------------------------------------------------------
-- Lookups
-- ----------------------------------------------------------------
CREATE TABLE roles (
    name VARCHAR(16) PRIMARY KEY
);

CREATE TABLE order_statuses (
    name VARCHAR(32) PRIMARY KEY
);

-- Seed lookup values (present in all environments)
INSERT INTO roles (name) VALUES ('USER'), ('MANAGER'), ('ADMIN'), ('SYNC');
INSERT INTO order_statuses (name) VALUES ('PENDING'), ('PAID'), ('SHIPPED'), ('DELIVERED'), ('CANCELLED');

-- ----------------------------------------------------------------
-- Loyalty tiers (referenced by users)
-- ----------------------------------------------------------------
CREATE TABLE loyalty_tiers (
    id                    BIGSERIAL      PRIMARY KEY,
    name                  VARCHAR(50)    NOT NULL UNIQUE,
    discount_percentage   NUMERIC(5,2)   NOT NULL DEFAULT 0,
    cashback_percentage   NUMERIC(5,2)   NOT NULL DEFAULT 0,
    min_spend_requirement NUMERIC(12,2)  NOT NULL DEFAULT 0,
    display_order         INT            NOT NULL DEFAULT 0,
    color                 VARCHAR(7)     NOT NULL DEFAULT '#6366F1',
    version               BIGINT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loyalty_tiers_display_order ON loyalty_tiers (display_order);

-- ----------------------------------------------------------------
-- Users
-- ----------------------------------------------------------------
CREATE TABLE users (
    id                     BIGSERIAL    PRIMARY KEY,
    phone                  VARCHAR(32)  NOT NULL UNIQUE,
    role                   VARCHAR(16)  NOT NULL REFERENCES roles(name),
    name                   VARCHAR(255),
    email                  VARCHAR(255) UNIQUE,
    enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    loyalty_points         INTEGER      NOT NULL DEFAULT 0,
    tier_id                BIGINT       REFERENCES loyalty_tiers(id),
    spend_to_next_tier     NUMERIC(12,2),
    spend_to_maintain_tier NUMERIC(12,2),
    current_month_spending NUMERIC(12,2),
    loyalty_permanent      BOOLEAN      NOT NULL DEFAULT FALSE,
    lowest_tier_ever_id    BIGINT       REFERENCES loyalty_tiers(id),
    version                BIGINT       NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;

-- ----------------------------------------------------------------
-- Categories
-- ----------------------------------------------------------------
CREATE TABLE categories (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(128) NOT NULL UNIQUE,
    slug       VARCHAR(128) NOT NULL UNIQUE,
    parent_id  BIGINT       REFERENCES categories(id),
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- Colors
-- ----------------------------------------------------------------
CREATE TABLE colors (
    id           BIGSERIAL    PRIMARY KEY,
    color_key    VARCHAR(64)  NOT NULL UNIQUE,
    display_name VARCHAR(128),
    hex_code     VARCHAR(7),
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- Brands
-- ----------------------------------------------------------------
CREATE TABLE brands (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(128) NOT NULL UNIQUE,
    logo_url   VARCHAR(512),
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
