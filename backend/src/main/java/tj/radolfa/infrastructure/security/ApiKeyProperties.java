package tj.radolfa.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for static API key authentication.
 * Bound to {@code radolfa.security.api-key.*} in application.yml.
 *
 * <p>The {@code system-key} is used by machine-to-machine clients (e.g. ERPNext)
 * that cannot participate in the OTP/JWT flow. A matching {@code X-Api-Key} header
 * grants the {@code SYSTEM} role.
 *
 * <p>Set via the {@code SYSTEM_API_KEY} environment variable in production.
 */
@ConfigurationProperties(prefix = "radolfa.security.api-key")
public record ApiKeyProperties(String systemKey) {

    private static final int MIN_KEY_LENGTH = 32;

    public ApiKeyProperties {
        if (systemKey == null || systemKey.length() < MIN_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SYSTEM_API_KEY must be at least " + MIN_KEY_LENGTH + " characters long");
        }
    }
}
