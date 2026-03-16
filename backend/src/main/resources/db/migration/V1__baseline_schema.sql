-- ================================================================
-- V1__baseline_schema.sql
--
-- Core infrastructure: roles, order statuses, users, sync log.
-- ================================================================

-- Lookup tables
CREATE TABLE roles (
    name VARCHAR(16) PRIMARY KEY
);

CREATE TABLE order_statuses (
    name VARCHAR(32) PRIMARY KEY
);

-- Users
CREATE TABLE users (
    id                      BIGSERIAL       PRIMARY KEY,
    phone                   VARCHAR(32)     NOT NULL UNIQUE,
    role                    VARCHAR(16)     NOT NULL REFERENCES roles(name),
    name                    VARCHAR(255),
    email                   VARCHAR(255)    UNIQUE,
    loyalty_points          INTEGER         NOT NULL DEFAULT 0,
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    tier_id                 BIGINT,
    spend_to_next_tier      NUMERIC(12,2),
    spend_to_maintain_tier  NUMERIC(12,2),
    current_month_spending  NUMERIC(12,2),
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;

-- ERP sync log
CREATE TABLE erp_sync_log (
    id            BIGSERIAL   PRIMARY KEY,
    erp_id        VARCHAR(64) NOT NULL,
    synced_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status        VARCHAR(16) NOT NULL CHECK (status IN ('SUCCESS', 'ERROR')),
    error_message TEXT
);

CREATE INDEX idx_erp_sync_log_erp_id    ON erp_sync_log (erp_id);
CREATE INDEX idx_erp_sync_log_synced_at ON erp_sync_log (synced_at);

-- ERP sync idempotency
CREATE TABLE erp_sync_idempotency (
    id              BIGSERIAL       PRIMARY KEY,
    idempotency_key VARCHAR(128)    NOT NULL,
    event_type      VARCHAR(32)     NOT NULL,
    response_status INT             NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_idempotency_key_event UNIQUE (idempotency_key, event_type)
);
