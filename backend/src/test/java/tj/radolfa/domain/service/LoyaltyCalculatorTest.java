package tj.radolfa.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.Money;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoyaltyCalculatorTest {

    private LoyaltyCalculator calculator;

    // Tiers: SILVER (spend ≥ 500), GOLD (spend ≥ 2000), PLATINUM (spend ≥ 5000)
    private LoyaltyTier silver;
    private LoyaltyTier gold;
    private LoyaltyTier platinum;
    private List<LoyaltyTier> allTiers;

    @BeforeEach
    void setUp() {
        calculator = new LoyaltyCalculator();

        silver   = new LoyaltyTier(1L, "Silver",   new BigDecimal("2"),  new BigDecimal("3"),  new BigDecimal("500"),  1, "#C0C0C0");
        gold     = new LoyaltyTier(2L, "Gold",     new BigDecimal("5"),  new BigDecimal("5"),  new BigDecimal("2000"), 2, "#FFD700");
        platinum = new LoyaltyTier(3L, "Platinum", new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("5000"), 3, "#E5E4E2");

        allTiers = List.of(silver, gold, platinum);
    }

    // ── awardPoints ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("No tier, spend below Silver threshold → no points, no tier")
    void awardPoints_noTier_belowAnyThreshold_staysNull() {
        LoyaltyProfile profile = LoyaltyProfile.empty();
        Money order = money("200"); // below Silver (500)

        LoyaltyProfile result = calculator.awardPoints(profile, order, allTiers);

        assertThat(result.points()).isZero();
        assertThat(result.currentMonthSpending()).isEqualByComparingTo("200");
        assertThat(result.tier()).isNull();
    }

    @Test
    @DisplayName("No tier but spend crosses Silver threshold → upgraded to Silver, no cashback (no prior tier)")
    void awardPoints_noTier_upgradesOnFirstSpend() {
        LoyaltyProfile profile = LoyaltyProfile.empty(); // no tier, 0 points, 0 spend
        Money order = money("600"); // crosses Silver (≥500)

        LoyaltyProfile result = calculator.awardPoints(profile, order, allTiers);

        // Tier upgrade happens based on new spend (600 ≥ 500)
        assertThat(result.tier()).isEqualTo(silver);
        // cashback was 0% (no prior tier) so 0 points awarded
        assertThat(result.points()).isZero();
        assertThat(result.currentMonthSpending()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("Silver tier 3% cashback on 1000 TJS → 30 points")
    void awardPoints_silverTier_awardsCorrectPoints() {
        LoyaltyProfile profile = new LoyaltyProfile(silver, 100, null, null, new BigDecimal("800"));
        Money order = money("1000");

        LoyaltyProfile result = calculator.awardPoints(profile, order, allTiers);

        // 3% of 1000 = 30 points; total = 100 + 30 = 130
        assertThat(result.points()).isEqualTo(130);
        assertThat(result.currentMonthSpending()).isEqualByComparingTo("1800");
        assertThat(result.tier()).isEqualTo(silver); // still below Gold threshold (2000)
    }

    @Test
    @DisplayName("Gold tier — spend crosses Platinum threshold → tier upgraded")
    void awardPoints_crossesPlatinumThreshold_upgradesTier() {
        // User is Gold, has spent 4800 this month, places 300 TJS order → total 5100 ≥ Platinum
        LoyaltyProfile profile = new LoyaltyProfile(gold, 200, null, null, new BigDecimal("4800"));
        Money order = money("300");

        LoyaltyProfile result = calculator.awardPoints(profile, order, allTiers);

        assertThat(result.tier()).isEqualTo(platinum);
        // 5% (Gold cashback) of 300 = 15 points; total = 200 + 15 = 215
        assertThat(result.points()).isEqualTo(215);
        assertThat(result.currentMonthSpending()).isEqualByComparingTo("5100");
        // Already at highest tier → spendToNextTier is null
        assertThat(result.spendToNextTier()).isNull();
    }

    @Test
    @DisplayName("awardPoints — spendToNextTier calculated correctly")
    void awardPoints_spendToNextTierIsCorrect() {
        LoyaltyProfile profile = new LoyaltyProfile(silver, 50, null, null, new BigDecimal("800"));
        Money order = money("500"); // new total = 1300, next tier (Gold) needs 2000

        LoyaltyProfile result = calculator.awardPoints(profile, order, allTiers);

        // spendToNextTier = 2000 - 1300 = 700
        assertThat(result.spendToNextTier()).isEqualByComparingTo("700");
    }

    // ── resolveDiscount ───────────────────────────────────────────────────────

    @Test
    @DisplayName("No tier → zero discount")
    void resolveDiscount_noTier_returnsZero() {
        LoyaltyProfile profile = LoyaltyProfile.empty();
        Money orderTotal = money("1000");

        Money discount = calculator.resolveDiscount(profile, orderTotal);

        assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Gold tier 5% discount on 800 TJS → 40 TJS")
    void resolveDiscount_goldTier_correctAmount() {
        LoyaltyProfile profile = new LoyaltyProfile(gold, 0, null, null, BigDecimal.ZERO);
        Money orderTotal = money("800");

        Money discount = calculator.resolveDiscount(profile, orderTotal);

        assertThat(discount.amount()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("Platinum tier 10% discount on 3000 TJS → 300 TJS")
    void resolveDiscount_platinumTier_correctAmount() {
        LoyaltyProfile profile = new LoyaltyProfile(platinum, 0, null, null, BigDecimal.ZERO);
        Money orderTotal = money("3000");

        Money discount = calculator.resolveDiscount(profile, orderTotal);

        assertThat(discount.amount()).isEqualByComparingTo("300.00");
    }

    // ── maxRedeemablePoints ───────────────────────────────────────────────────

    @Test
    @DisplayName("User has fewer points than the 30% cap → returns all user points")
    void maxRedeemablePoints_underCap_returnsAllPoints() {
        LoyaltyProfile profile = new LoyaltyProfile(gold, 50, null, null, BigDecimal.ZERO);
        Money orderTotal = money("1000"); // 30% cap = 300 points

        int redeemable = calculator.maxRedeemablePoints(profile, orderTotal);

        assertThat(redeemable).isEqualTo(50);
    }

    @Test
    @DisplayName("User has more points than the 30% cap → capped at 30% of order")
    void maxRedeemablePoints_overCap_cappedAtThirtyPercent() {
        LoyaltyProfile profile = new LoyaltyProfile(gold, 500, null, null, BigDecimal.ZERO);
        Money orderTotal = money("1000"); // 30% cap = 300 points

        int redeemable = calculator.maxRedeemablePoints(profile, orderTotal);

        assertThat(redeemable).isEqualTo(300);
    }

    @Test
    @DisplayName("Zero points → zero redeemable")
    void maxRedeemablePoints_zeroPoints_returnsZero() {
        LoyaltyProfile profile = LoyaltyProfile.empty();
        Money orderTotal = money("500");

        assertThat(calculator.maxRedeemablePoints(profile, orderTotal)).isZero();
    }

    // ── pointsToMoney ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("100 points → 100 TJS  (1 point = 1 TJS)")
    void pointsToMoney_conversionCorrect() {
        Money result = calculator.pointsToMoney(100);
        assertThat(result.amount()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Zero points → zero TJS")
    void pointsToMoney_zero() {
        assertThat(calculator.pointsToMoney(0).amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Negative points → IllegalArgumentException")
    void pointsToMoney_negative_throws() {
        assertThatThrownBy(() -> calculator.pointsToMoney(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount));
    }
}
