-- ================================================================
-- V14__pickpoint.sql
--
-- Pickpoint registry: physical pickup locations for order delivery.
-- Referenced (by id, no FK) from orders.pickpoint_id (added in V3).
-- ================================================================

CREATE TABLE pickpoint (
    id          BIGSERIAL      PRIMARY KEY,
    name        VARCHAR(255)   NOT NULL,
    address     TEXT           NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pickpoint_active ON pickpoint (active) WHERE active = TRUE;
CREATE INDEX idx_pickpoint_name   ON pickpoint (lower(name));
