package tj.radolfa.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;
import tj.radolfa.domain.model.WinningMechanism;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link CartLinePricer} resolves the same campaign-vs-loyalty decision
 * that {@code CheckoutService} relied on before extraction (see
 * {@code CheckoutServiceStackingTest} / {@code ResolveDiscountsServiceLoyaltyGuardTest}
 * for the end-to-end parity proof).
 */
class CartLinePricerTest {

    private static final Money ORIGINAL = new Money(new BigDecimal("100.00"));

    private final CartLinePricer pricer = new CartLinePricer();

    private static Discount percentDiscount(BigDecimal pct) {
        DiscountType type = new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);
        return new Discount(99L, type, List.of(new SkuTarget("SKU-1")),
                AmountType.PERCENT, pct,
                Instant.EPOCH, Instant.MAX, false, "Sale", "#F00",
                null, null, null, null);
    }

    @Test
    @DisplayName("No tier, no discount → original price, NONE")
    void noTierNoDiscount_returnsOriginal() {
        var result = pricer.price(ORIGINAL, BigDecimal.ZERO, List.of());

        assertThat(result.finalUnitPrice()).isEqualTo(new Money(new BigDecimal("100.00")));
        assertThat(result.mechanism()).isEqualTo(WinningMechanism.NONE);
        assertThat(result.effectivePercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.applied()).isEmpty();
    }

    @Test
    @DisplayName("Campaign cheaper than loyalty → CAMPAIGN wins")
    void campaignCheaperThanLoyalty_campaignWins() {
        // Loyalty: 10% off → 90.00. Campaign: 20% off → 80.00. Campaign wins.
        var result = pricer.price(ORIGINAL, new BigDecimal("10"), List.of(percentDiscount(new BigDecimal("20"))));

        assertThat(result.finalUnitPrice()).isEqualTo(new Money(new BigDecimal("80.00")));
        assertThat(result.mechanism()).isEqualTo(WinningMechanism.CAMPAIGN);
        assertThat(result.effectivePercent()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.applied()).hasSize(1);
    }

    @Test
    @DisplayName("Loyalty cheaper than campaign → LOYALTY wins, no discount recorded")
    void loyaltyCheaperThanCampaign_loyaltyWins() {
        // Loyalty: 25% off → 75.00. Campaign: 10% off → 90.00. Loyalty wins.
        var result = pricer.price(ORIGINAL, new BigDecimal("25"), List.of(percentDiscount(new BigDecimal("10"))));

        assertThat(result.finalUnitPrice()).isEqualTo(new Money(new BigDecimal("75.00")));
        assertThat(result.mechanism()).isEqualTo(WinningMechanism.LOYALTY);
        assertThat(result.effectivePercent()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.applied()).isEmpty();
    }

    @Test
    @DisplayName("Stacked price ties loyalty price → CAMPAIGN wins the tie")
    void tie_campaignWins() {
        // Loyalty: 20% off → 80.00. Campaign: 20% off → 80.00. Tie → campaign wins.
        var result = pricer.price(ORIGINAL, new BigDecimal("20"), List.of(percentDiscount(new BigDecimal("20"))));

        assertThat(result.finalUnitPrice()).isEqualTo(new Money(new BigDecimal("80.00")));
        assertThat(result.mechanism()).isEqualTo(WinningMechanism.CAMPAIGN);
        assertThat(result.applied()).hasSize(1);
    }

    @Test
    @DisplayName("Campaign present but does not beat the original price → NONE, original price")
    void campaignNotBeatingOriginal_returnsNone() {
        // 0% discount stacks to exactly the original price — never strictly beats it.
        var result = pricer.price(ORIGINAL, BigDecimal.ZERO, List.of(percentDiscount(BigDecimal.ZERO)));

        assertThat(result.finalUnitPrice()).isEqualTo(new Money(new BigDecimal("100.00")));
        assertThat(result.mechanism()).isEqualTo(WinningMechanism.NONE);
        assertThat(result.applied()).isEmpty();
    }
}
