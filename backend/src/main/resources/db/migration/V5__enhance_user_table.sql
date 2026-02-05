-- ================================================================
-- V5__enhance_user_table.sql  –  Add profile fields to users
-- ================================================================

-- Add new columns for user profile
ALTER TABLE users ADD COLUMN name VARCHAR(255);
ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Create index on email for faster lookups
CREATE INDEX idx_users_email ON users (email) WHERE email IS NOT NULL;

-- Update existing users with default updated_at
UPDATE users SET updated_at = created_at WHERE updated_at IS NULL;
