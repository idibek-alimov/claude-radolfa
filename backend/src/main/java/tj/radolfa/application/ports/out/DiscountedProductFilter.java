package tj.radolfa.application.ports.out;

import java.math.BigDecimal;

public record DiscountedProductFilter(
        String search,
        Long campaignId,
        BigDecimal minDeltaPercent,
        BigDecimal maxDeltaPercent
) {
    public static DiscountedProductFilter empty() {
        return new DiscountedProductFilter(null, null, null, null);
    }
}
