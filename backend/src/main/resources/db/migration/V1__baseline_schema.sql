-- ================================================================
-- V1__baseline_schema.sql
--
-- Core infrastructure tables: roles, order statuses, users, sync log.
-- ================================================================

-- ----------------------------------------------------------------
-- Lookup: roles
-- ----------------------------------------------------------------
CREATE TABLE roles (
    name VARCHAR(16) PRIMARY KEY
);

-- ----------------------------------------------------------------
-- Lookup: order_statuses
-- ----------------------------------------------------------------
CREATE TABLE order_statuses (
    name VARCHAR(32) PRIMARY KEY
);

-- ----------------------------------------------------------------
-- users
-- ----------------------------------------------------------------
CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    phone      VARCHAR(32)  NOT NULL UNIQUE,
    role       VARCHAR(16)  NOT NULL REFERENCES roles(name),
    name       VARCHAR(255),
    email      VARCHAR(255) UNIQUE,
    loyalty_points INTEGER  NOT NULL DEFAULT 0,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;

-- ----------------------------------------------------------------
-- erp_sync_log
-- ----------------------------------------------------------------
CREATE TABLE erp_sync_log (
    id            BIGSERIAL   PRIMARY KEY,
    erp_id        VARCHAR(64) NOT NULL,
    synced_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status        VARCHAR(16) NOT NULL CHECK (status IN ('SUCCESS', 'ERROR')),
    error_message TEXT
);

CREATE INDEX idx_erp_sync_log_erp_id    ON erp_sync_log (erp_id);
CREATE INDEX idx_erp_sync_log_synced_at ON erp_sync_log (synced_at);
