-- ================================================================
-- V3__commerce.sql
--
-- Orders, order items, discount types, discounts, and discount items.
--
-- Tables created here:
--   orders
--   order_items
--   discount_types
--   discounts
--   discount_items
-- ================================================================

-- ----------------------------------------------------------------
-- Orders
-- ----------------------------------------------------------------
CREATE TABLE orders (
    id                BIGSERIAL      PRIMARY KEY,
    user_id           BIGINT         NOT NULL REFERENCES users(id),
    external_order_id VARCHAR(64)    UNIQUE,
    status            VARCHAR(32)    NOT NULL DEFAULT 'PENDING' REFERENCES order_statuses(name),
    total_amount      NUMERIC(12,2)  NOT NULL DEFAULT 0,
    deleted_at        TIMESTAMPTZ,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id          ON orders (user_id);
CREATE INDEX idx_orders_active           ON orders (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_external_order_id ON orders (external_order_id) WHERE external_order_id IS NOT NULL;

-- ----------------------------------------------------------------
-- Order items
-- ----------------------------------------------------------------
CREATE TABLE order_items (
    id                BIGSERIAL      PRIMARY KEY,
    order_id          BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku_id            BIGINT         REFERENCES skus(id) ON DELETE SET NULL,
    sku_code          VARCHAR(128),
    product_name      VARCHAR(255),
    quantity          INTEGER        NOT NULL,
    price_at_purchase NUMERIC(12,2)  NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_sku_id   ON order_items (sku_id);

-- ----------------------------------------------------------------
-- Discount types (ADMIN-managed, rank determines conflict priority)
-- ----------------------------------------------------------------
CREATE TABLE discount_types (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(64)  NOT NULL UNIQUE,
    rank       INT          NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO discount_types (name, rank) VALUES
    ('FLASH_SALE',   1),
    ('CLEARANCE',    2),
    ('SEASONAL',     3),
    ('PROMOTIONAL',  4);

-- ----------------------------------------------------------------
-- Discounts
-- ----------------------------------------------------------------
CREATE TABLE discounts (
    id               BIGSERIAL      PRIMARY KEY,
    discount_type_id BIGINT         NOT NULL REFERENCES discount_types(id),
    discount_value   NUMERIC(5,2)   NOT NULL,
    valid_from       TIMESTAMPTZ    NOT NULL,
    valid_upto       TIMESTAMPTZ    NOT NULL,
    is_disabled      BOOLEAN        NOT NULL DEFAULT FALSE,
    title            VARCHAR(255),
    color_hex        VARCHAR(7),
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE discount_items (
    discount_id BIGINT      NOT NULL REFERENCES discounts(id) ON DELETE CASCADE,
    item_code   VARCHAR(64) NOT NULL,
    CONSTRAINT pk_discount_items PRIMARY KEY (discount_id, item_code)
);

CREATE INDEX idx_discount_items_item_code ON discount_items (item_code);
