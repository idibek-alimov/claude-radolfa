-- ================================================================
-- V1__init.sql  –  Initial schema for Radolfa
-- Flyway owns every DDL change.  hibernate.ddl-auto = validate.
-- ================================================================

-- ----------------------------------------------------------------
-- products
-- ----------------------------------------------------------------
CREATE TABLE products (
    id              BIGSERIAL   PRIMARY KEY,
    erp_id          VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(255),
    price           NUMERIC(12, 2),
    stock           INTEGER,
    web_description TEXT,
    is_top_selling  BOOLEAN     NOT NULL DEFAULT FALSE,
    images          TEXT[],                                  -- S3 URLs (Postgres array)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_erp_id ON products (erp_id);

-- ----------------------------------------------------------------
-- users
-- ----------------------------------------------------------------
CREATE TABLE users (
    id         BIGSERIAL   PRIMARY KEY,
    phone      VARCHAR(32) NOT NULL UNIQUE,
    role       VARCHAR(16) NOT NULL
                           CHECK (role IN ('USER', 'MANAGER', 'SYSTEM')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
