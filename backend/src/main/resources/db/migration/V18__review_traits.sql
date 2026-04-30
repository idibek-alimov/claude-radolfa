-- Trait bank: globally defined review questions
CREATE TABLE review_trait (
    id            BIGSERIAL    PRIMARY KEY,
    trait_key     VARCHAR(64)  NOT NULL UNIQUE,
    label_i18n    VARCHAR(255) NOT NULL,
    input_type    VARCHAR(16)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_review_trait_input_type CHECK (input_type IN ('SLIDER', 'RADIO'))
);

CREATE INDEX idx_review_trait_input_type ON review_trait (input_type);

-- Many-to-many: which categories use which traits
CREATE TABLE category_review_traits (
    category_id BIGINT NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    trait_id    BIGINT NOT NULL REFERENCES review_trait (id) ON DELETE CASCADE,
    PRIMARY KEY (category_id, trait_id)
);

CREATE INDEX idx_category_review_traits_trait_id ON category_review_traits (trait_id);

-- Review answers stored as JSONB keyed by trait_key
ALTER TABLE reviews ADD COLUMN trait_answers JSONB;
