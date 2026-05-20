CREATE TABLE notification_failures (
    id                BIGSERIAL    PRIMARY KEY,
    notification_type VARCHAR(50)  NOT NULL,
    user_id           BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    reference_id      BIGINT,
    error_message     TEXT,
    alert_sent        BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_failures_alert ON notification_failures(alert_sent) WHERE NOT alert_sent;
CREATE INDEX idx_notification_failures_type_ref ON notification_failures(notification_type, reference_id);
