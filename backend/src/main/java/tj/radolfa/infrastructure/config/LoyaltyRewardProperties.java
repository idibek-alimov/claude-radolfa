package tj.radolfa.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the loyalty programme.
 * Bound to {@code radolfa.loyalty.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.loyalty")
public record LoyaltyRewardProperties(
        /** Flat bonus credited to a user when their review is approved. */
        int reviewRewardPoints,
        /**
         * Calendar-month lifetime for every credit lot (earn / review bonus / restore).
         * Applied as {@code expires_at = earned_at + N months} (UTC).
         * Default: 12 months.
         */
        int pointsTtlMonths
) {}
