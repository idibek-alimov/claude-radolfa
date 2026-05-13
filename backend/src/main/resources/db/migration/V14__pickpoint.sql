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
    active                BOOLEAN        NOT NULL DEFAULT TRUE,
    latitude              DECIMAL(10, 8) NULL,
    longitude             DECIMAL(11, 8) NULL,
    has_parking           BOOLEAN        NOT NULL DEFAULT FALSE,
    has_fitting_room      BOOLEAN        NOT NULL DEFAULT FALSE,
    has_card_payment      BOOLEAN        NOT NULL DEFAULT FALSE,
    wheelchair_accessible BOOLEAN        NOT NULL DEFAULT FALSE,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pickpoint_active ON pickpoint (active) WHERE active = TRUE;
CREATE INDEX idx_pickpoint_name   ON pickpoint (lower(name));
CREATE INDEX idx_pickpoint_coords ON pickpoint (latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
