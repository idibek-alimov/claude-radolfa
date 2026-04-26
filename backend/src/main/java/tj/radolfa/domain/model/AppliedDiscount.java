package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * One discount applied to one SKU during resolution.
 * reducedUnitPrice is the cumulative per-unit price after this discount has been folded in
 * (i.e. after winner and all preceding stackable layers).
 */
public record AppliedDiscount(Discount discount, BigDecimal reducedUnitPrice) {

    /**
     * Folds an ordered discount list into applied discounts with cumulative prices.
     * PERCENT: running = running × (1 − value/100)
     * FIXED:   running = max(0, running − value)
     */
    public static List<AppliedDiscount> fold(List<Discount> ordered, BigDecimal originalPrice) {
        List<AppliedDiscount> result = new ArrayList<>(ordered.size());
        BigDecimal running = originalPrice;
        for (Discount d : ordered) {
            if (d.amountType() == AmountType.FIXED) {
                running = running.subtract(d.amountValue())
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                BigDecimal multiplier = BigDecimal.ONE.subtract(
                        d.amountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                running = running.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            }
            result.add(new AppliedDiscount(d, running));
        }
        return result;
    }
}
