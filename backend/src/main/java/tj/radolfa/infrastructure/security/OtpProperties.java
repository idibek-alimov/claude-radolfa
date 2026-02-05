package tj.radolfa.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OTP management.
 * Bound to {@code radolfa.security.otp.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.security.otp")
public record OtpProperties(
        int expirationSeconds,
        int length
) {}
