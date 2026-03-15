-- Add unique constraint on (idempotency_key, event_type) to prevent race conditions
ALTER TABLE erp_sync_idempotency
    ADD CONSTRAINT uq_idempotency_key_event_type UNIQUE (idempotency_key, event_type);
