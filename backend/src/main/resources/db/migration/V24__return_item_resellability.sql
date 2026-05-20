ALTER TABLE customer_return_items
    ADD COLUMN resellability VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW';
