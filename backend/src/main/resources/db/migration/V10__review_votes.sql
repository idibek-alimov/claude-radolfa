-- ================================================================
-- V10__review_votes.sql
--
-- Stores one vote (HELPFUL / NOT_HELPFUL) per user per review.
-- Upsert pattern: the UNIQUE constraint on (review_id, user_id)
-- ensures at most one vote per user; the application performs an
-- INSERT or UPDATE accordingly.
-- ================================================================

CREATE TABLE review_votes (
    id          BIGSERIAL    PRIMARY KEY,
    review_id   BIGINT       NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    vote        VARCHAR(16)  NOT NULL CHECK (vote IN ('HELPFUL', 'NOT_HELPFUL')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_review_vote_user UNIQUE (review_id, user_id)
);

CREATE INDEX idx_review_votes_review_id ON review_votes (review_id);
