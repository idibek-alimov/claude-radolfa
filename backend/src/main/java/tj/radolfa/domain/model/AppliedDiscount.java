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
     * Each entry's reducedUnitPrice = originalPrice × product of (1 - pct/100) up to that entry.
     */
    public static List<AppliedDiscount> fold(List<Discount> ordered, BigDecimal originalPrice) {
        List<AppliedDiscount> result = new ArrayList<>(ordered.size());
        BigDecimal running = originalPrice;
        for (Discount d : ordered) {
            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    d.amountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            running = running.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            result.add(new AppliedDiscount(d, running));
        }
        return result;
    }
}
