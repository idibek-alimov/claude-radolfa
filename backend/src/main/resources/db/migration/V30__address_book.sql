-- ================================================================
-- V30__address_book.sql
--
-- Customer address book — owner-scoped saved addresses with a
-- single exclusive default per user.
-- ================================================================

CREATE TABLE address_book (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label          VARCHAR(16)  NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    phone          VARCHAR(32)  NOT NULL,
    line1          VARCHAR(255) NOT NULL,
    city           VARCHAR(120) NOT NULL,
    postal_code    VARCHAR(32),
    country        VARCHAR(80)  NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_address_book_user_id ON address_book(user_id);

-- DB-level guarantee of the exclusive-default invariant: at most one default per user.
CREATE UNIQUE INDEX uq_address_book_user_default ON address_book(user_id) WHERE is_default;
