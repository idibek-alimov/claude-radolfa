package tj.radolfa.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configuration properties for checkout behaviour.
 * Bound to {@code radolfa.checkout.*} in application.yml.
 */
@ConfigurationProperties(prefix = "radolfa.checkout")
public record CheckoutProperties(
        BigDecimal codHandlingFee
) {}
