package tj.radolfa.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for payment webhook signature validation.
 * Bound to {@code radolfa.security.webhook.*} in application.yml.
 *
 * <p>The {@code secret} is the shared signing secret provided by the payment
 * gateway. When set, every incoming webhook must carry a valid
 * {@code X-Webhook-Signature} header (HMAC-SHA256 of the raw payload).
 *
 * <p>Set via the {@code WEBHOOK_SECRET} environment variable in production.
 * If left empty, signature validation is skipped — only acceptable in
 * local dev against the stub payment adapter.
 */
@ConfigurationProperties(prefix = "radolfa.security.webhook")
public record WebhookProperties(String secret) {

    public boolean isValidationEnabled() {
        return secret != null && !secret.isBlank();
    }
}
