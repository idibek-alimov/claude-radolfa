-- Drop legacy import/sync tables, Spring Batch schema, and SYNC role.
--
-- NOTE: As of V1__core_schema.sql, sync_log, import_idempotency, and
-- Spring Batch tables are no longer created in the baseline schema.
-- All DROP IF EXISTS statements below are intentional no-ops for clean
-- databases and exist only to handle databases migrated from an older baseline.

-- Drop sync log
DROP TABLE IF EXISTS sync_log CASCADE;

-- Drop import idempotency
DROP TABLE IF EXISTS import_idempotency CASCADE;

-- Drop Spring Batch schema
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT  CASCADE;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION          CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS    CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION           CASCADE;
DROP TABLE IF EXISTS BATCH_JOB_INSTANCE            CASCADE;

DROP SEQUENCE IF EXISTS BATCH_STEP_EXECUTION_SEQ;
DROP SEQUENCE IF EXISTS BATCH_JOB_EXECUTION_SEQ;
DROP SEQUENCE IF EXISTS BATCH_JOB_SEQ;

-- Remove SYNC role (no users should have this role in production, but guard with an update)
UPDATE users SET role = 'ADMIN' WHERE role = 'SYNC';
DELETE FROM roles WHERE name = 'SYNC';
