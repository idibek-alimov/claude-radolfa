package tj.radolfa.domain.service;

import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Pure domain service for loyalty programme calculations.
 *
 * <h3>Rules</h3>
 * <ul>
 *   <li><b>Points:</b> 1 point = 1 TJS. Cashback is awarded as points after a
 *       successful payment: {@code floor(orderAmount × cashbackPct / 100)}.</li>
 *   <li><b>Tier discount:</b> applied as a percentage off the order total at
 *       checkout.</li>
 *   <li><b>Redemption cap:</b> a user may redeem at most
 *       {@value #MAX_REDEMPTION_PCT}% of the order total in points per order.</li>
 *   <li><b>Tier upgrade:</b> determined by cumulative monthly spend. The highest
 *       tier whose {@code minSpendRequirement} ≤ current monthly spend is applied.</li>
 * </ul>
 *
 * <p>Pure Java — zero framework dependencies. Stateless; safe to share.
 */
public class LoyaltyCalculator {

    /** Conversion: 1 point = 1 TJS. */
    public static final int POINTS_PER_TJS = 1;

    /** Maximum fraction of an order total that can be covered by points (30 %). */
    public static final int MAX_REDEMPTION_PCT = 30;

    // ── Points Awarding ───────────────────────────────────────────────────────

    /**
     * Awards cashback points after a completed order and recalculates the tier.
     *
     * @param current     the user's current loyalty profile
     * @param orderAmount the paid order total (before any loyalty discount)
     * @param allTiers    all loyalty tier definitions, used for tier selection
     * @return a new {@link LoyaltyProfile} reflecting the updated state
     */
    public LoyaltyProfile awardPoints(LoyaltyProfile current,
                                      Money orderAmount,
                                      List<LoyaltyTier> allTiers) {
        // 1. Cashback points = floor(amount × cashback% / 100)
        BigDecimal cashbackPct = current.tier() != null && current.tier().cashbackPercentage() != null
                ? current.tier().cashbackPercentage()
                : BigDecimal.ZERO;

        int earnedPoints = orderAmount.amount()
                .multiply(cashbackPct)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .intValue();

        int totalPoints = current.points() + earnedPoints;

        // 2. Accumulate monthly spend
        BigDecimal prevMonthlySpend = current.currentMonthSpending() != null
                ? current.currentMonthSpending()
                : BigDecimal.ZERO;
        BigDecimal newMonthlySpend = prevMonthlySpend.add(orderAmount.amount());

        // 3. Determine new tier based on updated monthly spend
        LoyaltyTier newTier = determineTier(newMonthlySpend, allTiers, current.tier());

        // 4. Calculate how much more spend is needed for the next tier
        BigDecimal spendToNextTier = calculateSpendToNextTier(newMonthlySpend, newTier, allTiers);

        return new LoyaltyProfile(newTier, totalPoints, spendToNextTier, null, newMonthlySpend);
    }

    // ── Tier Discount ─────────────────────────────────────────────────────────

    /**
     * Calculates the monetary discount for a user's tier.
     *
     * @param profile    the user's current loyalty profile
     * @param orderTotal the order total before discount
     * @return the discount amount (may be {@link Money#ZERO} if no tier or no discount)
     */
    public Money resolveDiscount(LoyaltyProfile profile, Money orderTotal) {
        if (profile.tier() == null) return Money.ZERO;
        BigDecimal discountPct = profile.tier().discountPercentage();
        if (discountPct == null || discountPct.compareTo(BigDecimal.ZERO) == 0) {
            return Money.ZERO;
        }
        BigDecimal discountAmount = orderTotal.amount()
                .multiply(discountPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.FLOOR);
        return new Money(discountAmount);
    }

    // ── Points Redemption ─────────────────────────────────────────────────────

    /**
     * Returns the maximum number of points the user may redeem on this order.
     *
     * <p>Capped at the lower of:
     * <ul>
     *   <li>the user's available points, and</li>
     *   <li>{@value #MAX_REDEMPTION_PCT}% of the order total (1 point = 1 TJS).</li>
     * </ul>
     *
     * @param profile    the user's current loyalty profile
     * @param orderTotal the order total (after tier discount, before point redemption)
     * @return max redeemable points for this order
     */
    public int maxRedeemablePoints(LoyaltyProfile profile, Money orderTotal) {
        int pointCap = orderTotal.amount()
                .multiply(BigDecimal.valueOf(MAX_REDEMPTION_PCT))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .intValue();
        return Math.min(profile.points(), pointCap);
    }

    /**
     * Converts a points amount to its monetary equivalent.
     * 1 point = 1 TJS.
     */
    public Money pointsToMoney(int points) {
        if (points < 0) throw new IllegalArgumentException("points cannot be negative");
        return new Money(BigDecimal.valueOf((long) points * POINTS_PER_TJS));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns the highest tier whose {@code minSpendRequirement ≤ monthlySpend}.
     * Falls back to the current tier (or null) if no tier qualifies.
     */
    private LoyaltyTier determineTier(BigDecimal monthlySpend,
                                      List<LoyaltyTier> allTiers,
                                      LoyaltyTier currentTier) {
        if (allTiers == null || allTiers.isEmpty()) return currentTier;

        return allTiers.stream()
                .filter(t -> t.minSpendRequirement() != null
                        && t.minSpendRequirement().compareTo(monthlySpend) <= 0)
                .max(Comparator.comparing(LoyaltyTier::minSpendRequirement))
                .orElse(currentTier);
    }

    /**
     * How much more the user needs to spend this month to reach the next tier.
     * Returns null if they are already at the highest tier.
     */
    private BigDecimal calculateSpendToNextTier(BigDecimal monthlySpend,
                                                LoyaltyTier currentTier,
                                                List<LoyaltyTier> allTiers) {
        if (allTiers == null || allTiers.isEmpty()) return null;

        BigDecimal currentMin = currentTier != null && currentTier.minSpendRequirement() != null
                ? currentTier.minSpendRequirement()
                : BigDecimal.ZERO;

        return allTiers.stream()
                .filter(t -> t.minSpendRequirement() != null
                        && t.minSpendRequirement().compareTo(currentMin) > 0)
                .map(LoyaltyTier::minSpendRequirement)
                .min(Comparator.naturalOrder())
                .map(nextMin -> nextMin.subtract(monthlySpend).max(BigDecimal.ZERO))
                .orElse(null);
    }
}
