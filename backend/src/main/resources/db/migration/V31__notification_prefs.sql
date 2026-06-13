CREATE TABLE user_notification_prefs (
    user_id       BIGINT      PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    order_updates BOOLEAN     NOT NULL DEFAULT TRUE,
    promotions    BOOLEAN     NOT NULL DEFAULT TRUE,
    sms_messages  BOOLEAN     NOT NULL DEFAULT FALSE,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
