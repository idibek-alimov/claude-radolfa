-- ================================================================
-- V2__product_catalog.sql
--
-- Product catalog: sequences, product bases, listing variants,
-- images, attributes, category attribute blueprints, and SKUs.
--
-- Tables created here:
--   product_bases
--   listing_variants
--   listing_variant_images
--   listing_variant_attributes
--   category_attribute_blueprints
--   skus
-- ================================================================

-- ----------------------------------------------------------------
-- Sequence for listing variant product codes
-- ----------------------------------------------------------------
CREATE SEQUENCE listing_variant_code_seq START WITH 10001 INCREMENT BY 1;

-- ----------------------------------------------------------------
-- Product bases  (previously: erp_template_code → external_ref)
-- ----------------------------------------------------------------
CREATE TABLE product_bases (
    id            BIGSERIAL    PRIMARY KEY,
    external_ref  VARCHAR(64)  NOT NULL UNIQUE,
    name          VARCHAR(255),
    category_id   BIGINT       REFERENCES categories(id),
    category_name VARCHAR(255),
    brand_id      BIGINT       REFERENCES brands(id),
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_bases_external_ref ON product_bases (external_ref);

-- ----------------------------------------------------------------
-- Listing variants
-- ----------------------------------------------------------------
CREATE TABLE listing_variants (
    id              BIGSERIAL    PRIMARY KEY,
    product_base_id BIGINT       NOT NULL REFERENCES product_bases(id),
    color_id        BIGINT       NOT NULL REFERENCES colors(id),
    slug            VARCHAR(255) NOT NULL UNIQUE,
    web_description TEXT,
    top_selling     BOOLEAN      NOT NULL DEFAULT FALSE,
    featured        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
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
    is_primary         BOOLEAN     NOT NULL DEFAULT FALSE,
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
    sort_order         INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lva_variant_id ON listing_variant_attributes (listing_variant_id);

CREATE TABLE listing_variant_attribute_values (
    id           BIGSERIAL    PRIMARY KEY,
    attribute_id BIGINT       NOT NULL REFERENCES listing_variant_attributes(id) ON DELETE CASCADE,
    value        VARCHAR(512) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_lvav_attribute_id ON listing_variant_attribute_values (attribute_id);

-- ----------------------------------------------------------------
-- Category attribute blueprints (Wildberries-style attribute hints)
-- ----------------------------------------------------------------
CREATE TABLE category_attribute_blueprints (
    id            BIGSERIAL    PRIMARY KEY,
    category_id   BIGINT       NOT NULL REFERENCES categories(id),
    attribute_key VARCHAR(128) NOT NULL,
    type          VARCHAR(16)  NOT NULL DEFAULT 'TEXT',
    unit_name     VARCHAR(64),
    is_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blueprint_category_key UNIQUE (category_id, attribute_key)
);

CREATE INDEX idx_blueprint_category_id ON category_attribute_blueprints (category_id);

CREATE TABLE category_attribute_blueprint_values (
    id           BIGSERIAL    PRIMARY KEY,
    blueprint_id BIGINT       NOT NULL REFERENCES category_attribute_blueprints(id) ON DELETE CASCADE,
    allowed_value VARCHAR(256) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_cabv_blueprint_id ON category_attribute_blueprint_values (blueprint_id);

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
    barcode            VARCHAR(128)   UNIQUE,
    weight_kg          DOUBLE PRECISION,
    width_cm           INTEGER,
    height_cm          INTEGER,
    depth_cm           INTEGER,
    version            BIGINT         NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skus_variant_id ON skus (listing_variant_id);
CREATE INDEX idx_skus_sku_code   ON skus (sku_code);
