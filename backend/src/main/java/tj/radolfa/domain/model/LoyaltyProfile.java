package tj.radolfa.domain.model;

import java.math.BigDecimal;

public record LoyaltyProfile(
        LoyaltyTier tier,
        int points,
        BigDecimal spendToNextTier,
        BigDecimal spendToMaintainTier,
        BigDecimal currentMonthSpending,
        boolean permanent,
        LoyaltyTier lowestTierEver
) {
    public static LoyaltyProfile empty() {
        return new LoyaltyProfile(null, 0, null, null, null, false, null);
    }
}
