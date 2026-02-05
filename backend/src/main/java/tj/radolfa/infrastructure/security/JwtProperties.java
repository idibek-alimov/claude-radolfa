package tj.radolfa.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT token management.
 * Bound to {@code radolfa.security.jwt.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.security.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {}
