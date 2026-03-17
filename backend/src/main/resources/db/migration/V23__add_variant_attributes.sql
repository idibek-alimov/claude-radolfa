-- Product attributes table: enrichment key-value pairs for a listing variant.
-- Examples: Material=Organic Wool, Fit=Oversized, Care=Machine wash cold.
-- Owned by Radolfa content team — never overwritten by ERP sync.

CREATE TABLE listing_variant_attributes (
    id                  BIGSERIAL       PRIMARY KEY,
    listing_variant_id  BIGINT          NOT NULL REFERENCES listing_variants(id) ON DELETE CASCADE,
    attr_key            VARCHAR(128)    NOT NULL,
    attr_value          VARCHAR(512)    NOT NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lva_variant_id ON listing_variant_attributes(listing_variant_id);
