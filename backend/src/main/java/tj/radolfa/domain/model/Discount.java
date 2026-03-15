package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * An ERPNext Pricing Rule mapped to a specific item.
 *
 * <p>All fields are ERP-locked — synced from ERPNext via Server Script webhooks.
 * Active check is performed on-the-fly: {@link #isActive(Instant)}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public record Discount(
        Long id,
        String erpPricingRuleId,
        String itemCode,
        BigDecimal discountValue,
        Instant validFrom,
        Instant validUpto,
        boolean disabled
) {

    public Discount {
        Objects.requireNonNull(erpPricingRuleId, "erpPricingRuleId must not be null");
        Objects.requireNonNull(itemCode, "itemCode must not be null");
        Objects.requireNonNull(discountValue, "discountValue must not be null");
        Objects.requireNonNull(validFrom, "validFrom must not be null");
        Objects.requireNonNull(validUpto, "validUpto must not be null");
    }

    /**
     * Returns {@code true} if the discount is currently active:
     * not disabled, and {@code now} falls within [validFrom, validUpto].
     */
    public boolean isActive(Instant now) {
        if (disabled) return false;
        return !now.isBefore(validFrom) && !now.isAfter(validUpto);
    }
}
