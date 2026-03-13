package tj.radolfa.domain.model;

import java.math.BigDecimal;

public record LoyaltyProfile(
        LoyaltyTier tier,
        int points,
        BigDecimal spendToNextTier,
        BigDecimal spendToMaintainTier,
        BigDecimal currentMonthSpending
) {
    public static LoyaltyProfile empty() {
        return new LoyaltyProfile(null, 0, null, null, null);
    }
}
