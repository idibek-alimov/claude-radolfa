ALTER TABLE discount_types
    ADD COLUMN stacking_policy VARCHAR(16) NOT NULL DEFAULT 'BEST_WINS';
