-- ================================================================
-- V7__reviews_and_qa.sql
--
-- Customer reviews, review photos, rating summaries, and Q&A.
--
-- Tables created here:
--   reviews
--   review_photos
--   product_rating_summaries
--   product_questions
-- ================================================================

-- ----------------------------------------------------------------
-- Reviews
-- ----------------------------------------------------------------
CREATE TABLE reviews (
    id                   BIGSERIAL      PRIMARY KEY,
    listing_variant_id   BIGINT         NOT NULL REFERENCES listing_variants(id),
    sku_id               BIGINT         REFERENCES skus(id) ON DELETE SET NULL,
    order_id             BIGINT         NOT NULL REFERENCES orders(id),
    author_id            BIGINT         NOT NULL REFERENCES users(id),
    author_name          VARCHAR(128)   NOT NULL,
    rating               SMALLINT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title                VARCHAR(255),
    body                 TEXT,
    pros                 TEXT,
    cons                 TEXT,
    matching_size        VARCHAR(16),   -- ACCURATE | RUNS_SMALL | RUNS_LARGE | null
    status               VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
                                        -- PENDING | APPROVED | REJECTED
    seller_reply         TEXT,
    seller_replied_at    TIMESTAMPTZ,
    version              BIGINT         NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    -- A user can only review the same variant once per order
    CONSTRAINT uq_review_order_variant UNIQUE (order_id, listing_variant_id)
);

CREATE INDEX idx_reviews_variant_id ON reviews (listing_variant_id);
CREATE INDEX idx_reviews_author_id  ON reviews (author_id);
CREATE INDEX idx_reviews_status     ON reviews (status);

-- ----------------------------------------------------------------
-- Review photos (buyer-uploaded, processed via existing S3 pipeline)
-- ----------------------------------------------------------------
CREATE TABLE review_photos (
    id         BIGSERIAL  PRIMARY KEY,
    review_id  BIGINT     NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    url        TEXT       NOT NULL,
    sort_order INT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_review_photos_review_id ON review_photos (review_id);

-- ----------------------------------------------------------------
-- Product rating summaries (materialized per variant, recomputed on approval)
-- ----------------------------------------------------------------
CREATE TABLE product_rating_summaries (
    listing_variant_id   BIGINT         PRIMARY KEY REFERENCES listing_variants(id) ON DELETE CASCADE,
    average_rating       NUMERIC(3,2)   NOT NULL DEFAULT 0,
    review_count         INTEGER        NOT NULL DEFAULT 0,
    count_5              INTEGER        NOT NULL DEFAULT 0,
    count_4              INTEGER        NOT NULL DEFAULT 0,
    count_3              INTEGER        NOT NULL DEFAULT 0,
    count_2              INTEGER        NOT NULL DEFAULT 0,
    count_1              INTEGER        NOT NULL DEFAULT 0,
    size_accurate        INTEGER        NOT NULL DEFAULT 0,
    size_runs_small      INTEGER        NOT NULL DEFAULT 0,
    size_runs_large      INTEGER        NOT NULL DEFAULT 0,
    last_calculated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- Product questions (pre-purchase Q&A, base-product level)
-- ----------------------------------------------------------------
CREATE TABLE product_questions (
    id               BIGSERIAL    PRIMARY KEY,
    product_base_id  BIGINT       NOT NULL REFERENCES product_bases(id),
    author_id        BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    author_name      VARCHAR(128) NOT NULL,
    question_text    TEXT         NOT NULL,
    answer_text      TEXT,
    answered_at      TIMESTAMPTZ,
    status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
                                  -- PENDING | PUBLISHED | REJECTED
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_product_base_id ON product_questions (product_base_id);
CREATE INDEX idx_questions_status         ON product_questions (status);
