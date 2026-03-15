package tj.radolfa.application.services;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.domain.model.LoyaltyTier;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * Computes spend-to-next-tier for a user based on current tiers.
 *
 * Rules:
 * - If ERPNext provides an explicit value, it is used as-is.
 * - If the user has no tier, returns the gap to the lowest tier's minSpendRequirement.
 * - If the user has a tier, returns the gap to the next tier's minSpendRequirement.
 *   Returns null if the user is already on the highest tier.
 */
@Component
public class LoyaltySpendCalculator {

    private final LoadLoyaltyTierPort loadLoyaltyTierPort;

    public LoyaltySpendCalculator(LoadLoyaltyTierPort loadLoyaltyTierPort) {
        this.loadLoyaltyTierPort = loadLoyaltyTierPort;
    }

    public BigDecimal computeSpendToNextTier(LoyaltyTier resolvedTier,
                                             BigDecimal explicitSpendToNext,
                                             BigDecimal currentMonthSpending) {
        if (explicitSpendToNext != null) return explicitSpendToNext;

        var tiers = loadLoyaltyTierPort.findAll();
        if (tiers.isEmpty()) return null;

        BigDecimal spending = currentMonthSpending != null ? currentMonthSpending : BigDecimal.ZERO;

        if (resolvedTier == null) {
            LoyaltyTier lowestTier = tiers.stream()
                    .min(Comparator.comparingInt(LoyaltyTier::displayOrder))
                    .orElseThrow();
            BigDecimal gap = lowestTier.minSpendRequirement().subtract(spending);
            return gap.compareTo(BigDecimal.ZERO) > 0 ? gap : BigDecimal.ZERO;
        }

        return tiers.stream()
                .filter(t -> t.displayOrder() > resolvedTier.displayOrder())
                .min(Comparator.comparingInt(LoyaltyTier::displayOrder))
                .map(nextTier -> {
                    BigDecimal gap = nextTier.minSpendRequirement().subtract(spending);
                    return gap.compareTo(BigDecimal.ZERO) > 0 ? gap : BigDecimal.ZERO;
                })
                .orElse(null); // user is on the highest tier
    }
}
