package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Discount(
        Long id,
        DiscountType type,
        List<DiscountTarget> targets,
        AmountType amountType,
        BigDecimal amountValue,
        Instant validFrom,
        Instant validUpto,
        boolean disabled,
        String title,
        String colorHex,
        BigDecimal minBasketAmount,
        Integer usageCapTotal,
        Integer usageCapPerCustomer,
        String couponCode
) {

    public Discount {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(targets, "targets must not be null");
        Objects.requireNonNull(amountType, "amountType must not be null");
        Objects.requireNonNull(amountValue, "amountValue must not be null");
        Objects.requireNonNull(validFrom, "validFrom must not be null");
        Objects.requireNonNull(validUpto, "validUpto must not be null");
    }

    /** SKU codes from SKU-type targets only. Keeps existing call sites working. */
    public List<String> itemCodes() {
        return targets.stream()
                .filter(t -> t instanceof SkuTarget)
                .map(t -> ((SkuTarget) t).itemCode())
                .toList();
    }

    public boolean isActive(Instant now) {
        if (disabled) return false;
        return !now.isBefore(validFrom) && !now.isAfter(validUpto);
    }
}
