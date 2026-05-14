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
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the BEST_WINS comparator picks the discount with the highest
 * *actual currency reduction* at the item's price, not the raw amountValue.
 */
class ResolveDiscountsServiceBestPriceTest {

    private static final String SKU_A = "SKU-A";

    static final ExpandCategoryTargetPort NO_EXPAND  = cats -> List.of();
    static final LoadUserSegmentContextPort NO_SEGMENT = userId -> Optional.empty();
    static final QueryDiscountUsagePort NO_USAGE = new QueryDiscountUsagePort() {
        @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return Map.of(); }
        @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long u) { return Map.of(); }
    };

    static Discount percent(Long id, int rank, String pct) {
        DiscountType type = new DiscountType((long) rank, "TYPE-" + rank, rank, StackingPolicy.BEST_WINS);
        return new Discount(id, type, List.of(new SkuTarget(SKU_A)),
                AmountType.PERCENT, new BigDecimal(pct),
                Instant.EPOCH, Instant.MAX, false, "D" + id, "#000",
                null, null, null, null);
    }

    static Discount fixed(Long id, int rank, String amount) {
        DiscountType type = new DiscountType((long) rank, "TYPE-" + rank, rank, StackingPolicy.BEST_WINS);
        return new Discount(id, type, List.of(new SkuTarget(SKU_A)),
                AmountType.FIXED, new BigDecimal(amount),
                Instant.EPOCH, Instant.MAX, false, "D" + id, "#000",
                null, null, null, null);
    }

    ResolveDiscountsService build(List<Discount> discounts) {
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains(SKU_A) ? new ArrayList<>(discounts) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
            @Override public Optional<Discount> findByCouponCode(String code) { return Optional.empty(); }
        };
        return new ResolveDiscountsService(port, NO_EXPAND, NO_USAGE, NO_SEGMENT);
    }

    ResolveDiscountsUseCase.Query queryWithPrice(BigDecimal price) {
        return new ResolveDiscountsUseCase.Query(
                List.of(SKU_A), null, null, null, Map.of(SKU_A, price));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PERCENT 10 beats FIXED 50 on 1000 TJS: 100 TJS > 50 TJS")
    void percent10_beats_fixed50_on_1000() {
        Discount pct10  = percent(1L, 1, "10");  // 10% of 1000 = 100 TJS
        Discount fix50  = fixed(2L, 1, "50");    // 50 TJS flat, same rank

        Map<String, List<Discount>> result = build(List.of(fix50, pct10))
                .resolve(queryWithPrice(new BigDecimal("1000")));

        assertEquals(pct10, result.get(SKU_A).get(0), "PERCENT 10 (100 TJS) should beat FIXED 50");
    }

    @Test
    @DisplayName("FIXED 200 beats PERCENT 5 on 1000 TJS: 200 TJS > 50 TJS")
    void fixed200_beats_percent5_on_1000() {
        Discount pct5   = percent(1L, 1, "5");   // 5% of 1000 = 50 TJS
        Discount fix200 = fixed(2L, 1, "200");   // 200 TJS flat, same rank

        Map<String, List<Discount>> result = build(List.of(pct5, fix200))
                .resolve(queryWithPrice(new BigDecimal("1000")));

        assertEquals(fix200, result.get(SKU_A).get(0), "FIXED 200 (200 TJS) should beat PERCENT 5 (50 TJS)");
    }

    @Test
    @DisplayName("FIXED 500 vs FIXED 200 on 100 TJS: both cap at 100 TJS, lower id wins")
    void both_fix_cap_at_price_lower_id_wins() {
        Discount fix500 = fixed(10L, 1, "500");  // capped at 100 TJS
        Discount fix200 = fixed(20L, 1, "200");  // capped at 100 TJS

        Map<String, List<Discount>> result = build(List.of(fix200, fix500))
                .resolve(queryWithPrice(new BigDecimal("100")));

        assertEquals(fix500, result.get(SKU_A).get(0), "Equal reduction → lower id (10) wins");
    }

    @Test
    @DisplayName("Different ranks: lower rank wins even when its reduction is smaller")
    void lower_rank_wins_over_higher_reduction() {
        Discount rankOne = percent(1L, 1, "5");   // rank=1, 50 TJS on 1000
        Discount rankTwo = fixed(2L, 2, "200");   // rank=2, 200 TJS on 1000

        Map<String, List<Discount>> result = build(List.of(rankTwo, rankOne))
                .resolve(queryWithPrice(new BigDecimal("1000")));

        assertEquals(rankOne, result.get(SKU_A).get(0), "Rank dominates reduction size");
    }

    @Test
    @DisplayName("Empty price map: no exception, falls back to id tiebreak")
    void emptyPriceMap_noException_idTiebreak() {
        Discount pct10 = percent(1L, 1, "10");
        Discount fix50 = fixed(2L, 1, "50");     // same rank, id=2

        // 4-arg ctor → empty price map
        Map<String, List<Discount>> result = build(List.of(fix50, pct10))
                .resolve(new ResolveDiscountsUseCase.Query(List.of(SKU_A), null, null, null));

        assertNotNull(result.get(SKU_A));
        // Both reductions are 0 (no price) → tie-break falls to lower id
        assertEquals(pct10, result.get(SKU_A).get(0), "With no price, lower id (1) wins");
    }
}
