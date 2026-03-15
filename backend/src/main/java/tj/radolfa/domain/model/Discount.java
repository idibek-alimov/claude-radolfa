package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * An ERPNext Pricing Rule that can be applied to one or more SKU item codes.
 *
 * <p>All core fields are ERP-locked — synced from ERPNext via Server Script webhooks.
 * {@code title} and {@code colorHex} are UI display attributes also set during sync.
 * Active check is performed on-the-fly: {@link #isActive(Instant)}.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 */
public record Discount(
        Long id,
        String erpPricingRuleId,
        List<String> itemCodes,
        BigDecimal discountValue,
        Instant validFrom,
        Instant validUpto,
        boolean disabled,
        String title,
        String colorHex
) {

    public Discount {
        Objects.requireNonNull(erpPricingRuleId, "erpPricingRuleId must not be null");
        Objects.requireNonNull(itemCodes, "itemCodes must not be null");
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
