-- ================================================================
-- V1__baseline_clean_schema.sql
--
-- Single baseline for Radolfa. Consolidates all prior incremental
-- migrations into one clean schema with no erp-prefixed names.
--
-- Tables created here:
--   roles, order_statuses
--   users, loyalty_tiers
--   categories, colors
--   product_bases, listing_variants, listing_variant_images
--   listing_variant_attributes
--   skus
--   orders, order_items
--   discounts, discount_items
--   sync_log
--   import_idempotency
--   Spring Batch infrastructure
--   listing_variant_code_seq
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
-- Product bases  (previously: erp_template_code → external_ref)
-- ----------------------------------------------------------------
CREATE TABLE product_bases (
    id            BIGSERIAL    PRIMARY KEY,
    external_ref  VARCHAR(64)  NOT NULL UNIQUE,
    name          VARCHAR(255),
    category_id   BIGINT       REFERENCES categories(id),
    category_name VARCHAR(255),
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_bases_external_ref ON product_bases (external_ref);

-- ----------------------------------------------------------------
-- Listing variants
-- ----------------------------------------------------------------
CREATE SEQUENCE listing_variant_code_seq START WITH 10001 INCREMENT BY 1;

CREATE TABLE listing_variants (
    id              BIGSERIAL    PRIMARY KEY,
    product_base_id BIGINT       NOT NULL REFERENCES product_bases(id),
    color_id        BIGINT       NOT NULL REFERENCES colors(id),
    slug            VARCHAR(255) NOT NULL UNIQUE,
    web_description TEXT,
    top_selling     BOOLEAN      NOT NULL DEFAULT FALSE,
    featured        BOOLEAN      NOT NULL DEFAULT FALSE,
    last_sync_at    TIMESTAMPTZ,
    product_code    VARCHAR(10)  DEFAULT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_variant_base_color UNIQUE (product_base_id, color_id)
);

CREATE INDEX idx_listing_variants_base_id  ON listing_variants (product_base_id);
CREATE INDEX idx_listing_variants_slug     ON listing_variants (slug);
CREATE INDEX idx_listing_variants_featured ON listing_variants (featured) WHERE featured = TRUE;
CREATE UNIQUE INDEX uq_listing_variant_product_code ON listing_variants (product_code);

-- ----------------------------------------------------------------
-- Listing variant images
-- ----------------------------------------------------------------
CREATE TABLE listing_variant_images (
    id                 BIGSERIAL   PRIMARY KEY,
    listing_variant_id BIGINT      NOT NULL REFERENCES listing_variants(id) ON DELETE CASCADE,
    image_url          TEXT        NOT NULL,
    sort_order         INTEGER     NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_variant_images_variant_id ON listing_variant_images (listing_variant_id);

-- ----------------------------------------------------------------
-- Listing variant attributes
-- ----------------------------------------------------------------
CREATE TABLE listing_variant_attributes (
    id                 BIGSERIAL    PRIMARY KEY,
    listing_variant_id BIGINT       NOT NULL REFERENCES listing_variants(id) ON DELETE CASCADE,
    attr_key           VARCHAR(128) NOT NULL,
    attr_value         VARCHAR(512) NOT NULL,
    sort_order         INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lva_variant_id ON listing_variant_attributes (listing_variant_id);

-- ----------------------------------------------------------------
-- SKUs  (previously: erp_item_code → sku_code)
-- ----------------------------------------------------------------
CREATE TABLE skus (
    id                 BIGSERIAL      PRIMARY KEY,
    listing_variant_id BIGINT         NOT NULL REFERENCES listing_variants(id),
    sku_code           VARCHAR(64)    NOT NULL UNIQUE,
    size_label         VARCHAR(32),
    stock_quantity     INTEGER        NOT NULL DEFAULT 0,
    original_price     NUMERIC(12,2),
    version            BIGINT         NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skus_variant_id ON skus (listing_variant_id);
CREATE INDEX idx_skus_sku_code   ON skus (sku_code);

-- ----------------------------------------------------------------
-- Orders  (previously: erp_order_id → external_order_id)
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
-- Order items  (previously: erp_item_code → sku_code)
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
-- Discounts  (previously: erp_pricing_rule_id → external_rule_id)
-- ----------------------------------------------------------------
CREATE TABLE discounts (
    id               BIGSERIAL      PRIMARY KEY,
    external_rule_id VARCHAR(140)   NOT NULL UNIQUE,
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

-- ----------------------------------------------------------------
-- Sync log  (previously: erp_sync_log)
-- ----------------------------------------------------------------
CREATE TABLE sync_log (
    id            BIGSERIAL   PRIMARY KEY,
    import_id     VARCHAR(64) NOT NULL,
    synced_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status        VARCHAR(16) NOT NULL CHECK (status IN ('SUCCESS', 'ERROR')),
    error_message TEXT
);

CREATE INDEX idx_sync_log_import_id ON sync_log (import_id);
CREATE INDEX idx_sync_log_synced_at ON sync_log (synced_at);

-- ----------------------------------------------------------------
-- Import idempotency  (previously: erp_sync_idempotency)
-- ----------------------------------------------------------------
CREATE TABLE import_idempotency (
    id              BIGSERIAL    PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    event_type      VARCHAR(32)  NOT NULL,
    response_status INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_idempotency_key_event UNIQUE (idempotency_key, event_type)
);

-- ----------------------------------------------------------------
-- Spring Batch infrastructure
-- Managed here because spring.batch.jdbc.initialize-schema=never
-- ----------------------------------------------------------------
CREATE TABLE BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT        NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100)  NOT NULL,
    JOB_KEY         VARCHAR(32)   NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT       NOT NULL PRIMARY KEY,
    VERSION          BIGINT,
    JOB_INSTANCE_ID  BIGINT       NOT NULL,
    CREATE_TIME      TIMESTAMP    NOT NULL,
    START_TIME       TIMESTAMP    DEFAULT NULL,
    END_TIME         TIMESTAMP    DEFAULT NULL,
    STATUS           VARCHAR(10),
    EXIT_CODE        VARCHAR(2500),
    EXIT_MESSAGE     VARCHAR(2500),
    LAST_UPDATED     TIMESTAMP,
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID)
        REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT        NOT NULL,
    PARAMETER_NAME   VARCHAR(100)  NOT NULL,
    PARAMETER_TYPE   VARCHAR(100)  NOT NULL,
    PARAMETER_VALUE  VARCHAR(2500),
    IDENTIFYING      CHAR(1)       NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    CREATE_TIME        TIMESTAMP    NOT NULL,
    START_TIME         TIMESTAMP    DEFAULT NULL,
    END_TIME           TIMESTAMP    DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       TIMESTAMP,
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID)
        REFERENCES BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ  MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_SEQ            MAXVALUE 9223372036854775807 NO CYCLE;
