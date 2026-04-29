ALTER TABLE reviews ADD COLUMN upvotes INTEGER NOT NULL DEFAULT 0;

UPDATE reviews r SET upvotes = (
    SELECT COUNT(*) FROM review_votes v
    WHERE v.review_id = r.id AND v.vote = 'HELPFUL'
);

CREATE INDEX idx_reviews_upvotes ON reviews (upvotes DESC);
