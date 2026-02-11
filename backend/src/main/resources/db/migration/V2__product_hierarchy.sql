-- ================================================================
-- V2__product_hierarchy.sql
--
-- 3-tier product schema:
--   ProductBase (Template) -> ListingVariant (Colour) -> Sku (Size)
-- ================================================================

-- ----------------------------------------------------------------
-- product_bases (ERPNext Item Template)
-- ----------------------------------------------------------------
CREATE TABLE product_bases (
    id                  BIGSERIAL       PRIMARY KEY,
    erp_template_code   VARCHAR(64)     NOT NULL UNIQUE,
    name                VARCHAR(255),
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_bases_template_code ON product_bases (erp_template_code);

-- ----------------------------------------------------------------
-- listing_variants (Colour variant — the storefront display unit)
-- ----------------------------------------------------------------
CREATE TABLE listing_variants (
    id                  BIGSERIAL       PRIMARY KEY,
    product_base_id     BIGINT          NOT NULL REFERENCES product_bases(id),
    color_key           VARCHAR(64)     NOT NULL,
    slug                VARCHAR(255)    NOT NULL UNIQUE,
    web_description     TEXT,
    top_selling         BOOLEAN         NOT NULL DEFAULT FALSE,
    last_sync_at        TIMESTAMPTZ,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    UNIQUE (product_base_id, color_key)
);

CREATE INDEX idx_listing_variants_base_id ON listing_variants (product_base_id);
CREATE INDEX idx_listing_variants_slug    ON listing_variants (slug);

-- ----------------------------------------------------------------
-- listing_variant_images (S3 image URLs for a variant)
-- ----------------------------------------------------------------
CREATE TABLE listing_variant_images (
    id                    BIGSERIAL     PRIMARY KEY,
    listing_variant_id    BIGINT        NOT NULL REFERENCES listing_variants(id) ON DELETE CASCADE,
    image_url             TEXT          NOT NULL,
    sort_order            INTEGER       NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_variant_images_variant_id ON listing_variant_images (listing_variant_id);

-- ----------------------------------------------------------------
-- skus (Size/price variant — the purchasable unit)
-- ----------------------------------------------------------------
CREATE TABLE skus (
    id                    BIGSERIAL       PRIMARY KEY,
    listing_variant_id    BIGINT          NOT NULL REFERENCES listing_variants(id),
    erp_item_code         VARCHAR(64)     NOT NULL UNIQUE,
    size_label            VARCHAR(32),
    stock_quantity        INTEGER         NOT NULL DEFAULT 0,
    price                 NUMERIC(12, 2),
    sale_price            NUMERIC(12, 2),
    sale_ends_at          TIMESTAMPTZ,
    version               BIGINT          NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skus_variant_id    ON skus (listing_variant_id);
CREATE INDEX idx_skus_erp_item_code ON skus (erp_item_code);
