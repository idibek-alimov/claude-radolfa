package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.ExpandCategoryTargetPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadUserSegmentContextPort;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.AppliedDiscount;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards against regression: pre-Phase-10 SKU-only percent-only campaigns must
 * produce exactly the same winner and final price as direct manual computation.
 */
class ResolveDiscountsServiceLegacyEquivalenceTest {

    static final ExpandCategoryTargetPort NO_EXPAND = cats -> List.of();
    static final LoadUserSegmentContextPort NO_SEGMENT = userId -> Optional.empty();
    static final QueryDiscountUsagePort NO_USAGE = new QueryDiscountUsagePort() {
        @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return Map.of(); }
        @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long u) { return Map.of(); }
    };

    static Discount skuDiscount(Long id, int rank, BigDecimal pct) {
        DiscountType type = new DiscountType((long) rank, "TYPE", rank, StackingPolicy.BEST_WINS);
        return new Discount(id, type,
                List.of(new SkuTarget("SKU-LEGACY")),
                AmountType.PERCENT, pct,
                Instant.EPOCH, Instant.MAX, false, "Legacy", "#000",
                null, null, null, null);
    }

    ResolveDiscountsService build(List<Discount> discounts) {
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains("SKU-LEGACY") ? discounts : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };
        return new ResolveDiscountsService(port, NO_EXPAND, NO_USAGE, NO_SEGMENT);
    }

    @Test
    @DisplayName("Single SKU-only PERCENT BEST_WINS discount: final price equals manual pct calculation")
    void singleSkuDiscount_matchesManualComputation() {
        BigDecimal originalPrice = new BigDecimal("250.00");
        BigDecimal pct = new BigDecimal("15");
        Discount d = skuDiscount(1L, 1, pct);

        Map<String, List<Discount>> result = build(List.of(d)).resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-LEGACY"), null, null));

        List<Discount> ordered = result.get("SKU-LEGACY");
        assertEquals(1, ordered.size());
        assertEquals(d, ordered.get(0));

        // Manual: 250 × (1 - 15/100) = 250 × 0.85 = 212.50
        BigDecimal expected = originalPrice
                .multiply(BigDecimal.ONE.subtract(pct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        List<AppliedDiscount> applied = AppliedDiscount.fold(ordered, originalPrice);
        assertEquals(expected, applied.get(0).reducedUnitPrice());
        assertEquals(new BigDecimal("212.50"), applied.get(0).reducedUnitPrice());
    }

    @Test
    @DisplayName("Two competing BEST_WINS discounts: rank-winner result is same as if only winner existed")
    void twoCompetingBestWins_winnerResultIdentical() {
        Discount winner = skuDiscount(1L, 1, new BigDecimal("20"));
        Discount loser  = skuDiscount(2L, 2, new BigDecimal("25")); // higher % but lower priority

        Map<String, List<Discount>> result = build(List.of(winner, loser)).resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-LEGACY"), null, null));

        List<Discount> ordered = result.get("SKU-LEGACY");
        assertEquals(1, ordered.size(), "Only winner (rank=1) should survive");
        assertEquals(winner, ordered.get(0));

        BigDecimal original = new BigDecimal("100.00");
        List<AppliedDiscount> applied = AppliedDiscount.fold(ordered, original);
        assertEquals(new BigDecimal("80.00"), applied.get(0).reducedUnitPrice());
    }
}
