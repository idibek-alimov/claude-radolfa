INSERT INTO order_statuses (name) VALUES ('RECALL_REQUESTED');

ALTER TABLE orders
    ADD COLUMN recall_requested_at         TIMESTAMPTZ,
    ADD COLUMN recall_requested_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN recall_reason               TEXT,
    ADD COLUMN recall_confirmed_at         TIMESTAMPTZ,
    ADD COLUMN recall_confirmed_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
