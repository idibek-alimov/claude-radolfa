CREATE TABLE home_banners (
    id           BIGSERIAL    PRIMARY KEY,
    slot         VARCHAR(16)  NOT NULL,
    title        VARCHAR(160) NOT NULL,
    subtitle     VARCHAR(255),
    badge_text   VARCHAR(80),
    cta_label    VARCHAR(80),
    cta_url      VARCHAR(500),
    bg_color_hex VARCHAR(9),
    expires_at   TIMESTAMPTZ,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_home_banners_slot_active ON home_banners(slot, active);
