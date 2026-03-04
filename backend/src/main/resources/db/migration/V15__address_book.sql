-- ================================================================
-- V15__address_book.sql
--
-- Persistent address book. One book per user (UNIQUE on user_id).
-- Addresses belong to one book; only one can be is_default=TRUE.
-- The single-default invariant is enforced in the domain layer.
-- ================================================================

CREATE TABLE address_books (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE addresses (
    id               BIGSERIAL    PRIMARY KEY,
    address_book_id  BIGINT       NOT NULL REFERENCES address_books(id) ON DELETE CASCADE,
    label            VARCHAR(64)  NOT NULL,
    recipient_name   VARCHAR(128) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    street           TEXT         NOT NULL,
    city             VARCHAR(128) NOT NULL,
    region           VARCHAR(64)  NOT NULL,
    country          VARCHAR(64)  NOT NULL DEFAULT 'Tajikistan',
    is_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_address_book_label UNIQUE (address_book_id, label)
);

CREATE INDEX idx_addresses_book_id ON addresses(address_book_id);
