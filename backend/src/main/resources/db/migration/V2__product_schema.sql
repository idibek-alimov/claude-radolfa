-- ================================================================
-- V2__product_schema.sql
--
-- 2-layer product structure mirroring ERPNext 15:
--   ProductTemplate  -> ERPNext Item (has_variants=1 or standalone)
--   ProductVariant   -> ERPNext Item Variant (or standalone's single row)
--
-- Plus lookup tables: categories, colors (hex code registry).
-- ================================================================

-- ----------------------------------------------------------------
-- categories (hierarchical, synced from ERPNext Item Groups)
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
-- colors (lookup for hex codes / display names on swatches)
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
-- product_templates
--
-- One row per ERPNext Item template (has_variants=1),
-- or one row per standalone item (no variants).
-- ----------------------------------------------------------------
CREATE TABLE product_templates (
    id                      BIGSERIAL       PRIMARY KEY,
    erp_template_code       VARCHAR(64)     NOT NULL UNIQUE,
    name                    VARCHAR(255),
    description             TEXT,
    category_id             BIGINT          REFERENCES categories(id),
    category_name           VARCHAR(255),
    attributes_definition   JSONB           NOT NULL DEFAULT '{}'::jsonb,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    top_selling             BOOLEAN         NOT NULL DEFAULT FALSE,
    featured                BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 BIGINT          NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pt_erp_code    ON product_templates (erp_template_code);
CREATE INDEX idx_pt_category    ON product_templates (category_id);
CREATE INDEX idx_pt_active      ON product_templates (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_pt_attrs_def   ON product_templates USING gin (attributes_definition);

-- ----------------------------------------------------------------
-- product_variants
--
-- One row per ERPNext Item Variant (specific color+size combo).
-- Standalone items get a single variant with attributes = '{}'.
--
-- Images stored as JSONB array: ["https://s3.../img1.jpg", ...]
-- Attributes stored as JSONB object: {"Color": "Red", "Size": "M"}
-- ----------------------------------------------------------------
CREATE TABLE product_variants (
    id                  BIGSERIAL       PRIMARY KEY,
    erp_variant_code    VARCHAR(64)     NOT NULL UNIQUE,
    template_id         BIGINT          NOT NULL REFERENCES product_templates(id),
    attributes          JSONB           NOT NULL DEFAULT '{}'::jsonb,
    price               NUMERIC(12, 2),
    stock_qty           INTEGER         NOT NULL DEFAULT 0,
    seo_slug            VARCHAR(255)    UNIQUE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    last_sync_at        TIMESTAMPTZ,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pv_erp_code       ON product_variants (erp_variant_code);
CREATE INDEX idx_pv_template       ON product_variants (template_id);
CREATE INDEX idx_pv_slug           ON product_variants (seo_slug);
CREATE INDEX idx_pv_active         ON product_variants (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_pv_attrs          ON product_variants USING gin (attributes);
CREATE INDEX idx_pv_template_color ON product_variants (template_id, ((attributes->>'Color')));
