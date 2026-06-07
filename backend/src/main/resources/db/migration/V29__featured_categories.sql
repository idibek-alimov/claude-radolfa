CREATE TABLE featured_categories (
    id            BIGSERIAL    PRIMARY KEY,
    category_id   BIGINT       NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    image_url     VARCHAR(512),
    title         VARCHAR(160),
    subtitle      VARCHAR(255),
    display_order INT          NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_featured_categories_active_order ON featured_categories(active, display_order);
CREATE INDEX idx_featured_categories_category     ON featured_categories(category_id);
