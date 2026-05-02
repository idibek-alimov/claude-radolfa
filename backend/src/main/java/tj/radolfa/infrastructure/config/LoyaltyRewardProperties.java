package tj.radolfa.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for loyalty reward amounts.
 * Bound to {@code radolfa.loyalty.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.loyalty")
public record LoyaltyRewardProperties(
        int reviewRewardPoints
) {}
