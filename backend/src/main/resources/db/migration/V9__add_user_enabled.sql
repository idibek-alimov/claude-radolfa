-- Add enabled flag for user account status management
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
