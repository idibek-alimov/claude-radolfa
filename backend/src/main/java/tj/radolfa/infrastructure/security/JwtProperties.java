package tj.radolfa.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT token management.
 * Bound to {@code radolfa.security.jwt.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.security.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        long refreshExpirationMs
) {
    private static final int MIN_SECRET_LENGTH = 32;

    public JwtProperties {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least " + MIN_SECRET_LENGTH + " characters long");
        }
    }
}
