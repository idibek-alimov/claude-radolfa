package tj.radolfa.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for auth endpoint rate limiting.
 * Bound to {@code radolfa.security.rate-limit.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.security.rate-limit")
public record RateLimitProperties(
        int otpRequestMaxPerPhone,
        int otpRequestWindowMinutes,
        int otpVerifyMaxPerPhone,
        int otpVerifyWindowMinutes,
        int ipMaxPerHour
) {}
