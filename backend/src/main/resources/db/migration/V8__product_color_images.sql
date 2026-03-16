-- ----------------------------------------------------------------
-- product_color_images
--
-- One image gallery per (template, color).
-- color_key IS NULL for standalone / colorless products.
-- Images stored as JSONB array: ["https://s3.../img1.webp", ...]
-- ----------------------------------------------------------------
CREATE TABLE product_color_images (
    id          BIGSERIAL       PRIMARY KEY,
    template_id BIGINT          NOT NULL REFERENCES product_templates(id),
    color_key   VARCHAR(64),
    images      JSONB           NOT NULL DEFAULT '[]'::jsonb,
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (template_id, color_key)
);

CREATE INDEX idx_pci_template ON product_color_images (template_id);
CREATE INDEX idx_pci_template_color ON product_color_images (template_id, color_key);
