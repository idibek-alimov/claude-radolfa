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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolveDiscountsServiceStackingTest {

    // ---- Fakes ----

    static final ExpandCategoryTargetPort NO_EXPAND = cats -> List.of();
    static final LoadUserSegmentContextPort NO_SEGMENT = userId -> Optional.empty();
    static final QueryDiscountUsagePort NO_USAGE = new QueryDiscountUsagePort() {
        @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return Map.of(); }
        @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long u) { return Map.of(); }
    };

    // ---- Fixture helpers ----

    static Discount disc(Long id, int rank, BigDecimal pct, StackingPolicy policy) {
        DiscountType type = new DiscountType((long) rank, "TYPE-" + rank, rank, policy);
        return new Discount(id, type,
                List.of(new SkuTarget("SKU-A")),
                AmountType.PERCENT, pct,
                Instant.EPOCH, Instant.MAX, false, "D" + id, "#000",
                null, null, null, null);
    }

    ResolveDiscountsService build(List<Discount> discounts) {
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains("SKU-A") ? new ArrayList<>(discounts) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
            @Override public Optional<Discount> findByCouponCode(String code) { return Optional.empty(); }
        };
        return new ResolveDiscountsService(port, NO_EXPAND, NO_USAGE, NO_SEGMENT);
    }

    // ---- Tests ----

    @Test
    @DisplayName("BEST_WINS(20%) + STACKABLE(10%) on 100 → final=72.00, two applied entries")
    void bestWinsWinner_plusStackable_finalPrice72() {
        Discount winner = disc(1L, 1, new BigDecimal("20"), StackingPolicy.BEST_WINS);   // rank=1
        Discount stacker = disc(2L, 2, new BigDecimal("10"), StackingPolicy.STACKABLE);  // rank=2

        Map<String, List<Discount>> result = build(List.of(winner, stacker))
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, null));

        List<Discount> ordered = result.get("SKU-A");
        assertEquals(2, ordered.size(), "winner + 1 stackable");
        assertEquals(winner, ordered.get(0));
        assertEquals(stacker, ordered.get(1));

        // Verify price folding
        List<AppliedDiscount> applied = AppliedDiscount.fold(ordered, new BigDecimal("100.00"));
        assertEquals(new BigDecimal("80.00"), applied.get(0).reducedUnitPrice()); // after 20%
        assertEquals(new BigDecimal("72.00"), applied.get(1).reducedUnitPrice()); // after 10% on 80
    }

    @Test
    @DisplayName("Two BEST_WINS campaigns: lower rank (higher priority) wins")
    void twoBestWins_lowerRankWins() {
        Discount highPriority = disc(1L, 1, new BigDecimal("15"), StackingPolicy.BEST_WINS); // rank=1
        Discount lowPriority  = disc(2L, 3, new BigDecimal("30"), StackingPolicy.BEST_WINS); // rank=3

        Map<String, List<Discount>> result = build(List.of(lowPriority, highPriority))
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, null));

        List<Discount> ordered = result.get("SKU-A");
        assertEquals(1, ordered.size(), "Only the BEST_WINS winner");
        assertEquals(highPriority, ordered.get(0), "rank=1 beats rank=3 regardless of % size");
    }

    @Test
    @DisplayName("Tie-breaking by id when rank is equal: lower id wins")
    void tieBreak_byId_lowerIdWins() {
        Discount first  = disc(10L, 1, new BigDecimal("20"), StackingPolicy.BEST_WINS);
        Discount second = disc(20L, 1, new BigDecimal("20"), StackingPolicy.BEST_WINS);

        Map<String, List<Discount>> result = build(List.of(second, first))
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, null));

        assertEquals(first, result.get("SKU-A").get(0), "id=10 beats id=20 on tie");
    }

    @Test
    @DisplayName("STACKABLE-only campaigns all apply in rank order")
    void stackableOnly_allApplyInRankOrder() {
        Discount s1 = disc(1L, 1, new BigDecimal("10"), StackingPolicy.STACKABLE);
        Discount s2 = disc(2L, 2, new BigDecimal("5"),  StackingPolicy.STACKABLE);

        Map<String, List<Discount>> result = build(List.of(s2, s1))
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, null));

        List<Discount> ordered = result.get("SKU-A");
        assertEquals(2, ordered.size());
        assertEquals(s1, ordered.get(0)); // rank=1 first
        assertEquals(s2, ordered.get(1)); // rank=2 second
    }

    @Test
    @DisplayName("No discount on unrequested item code")
    void noMatch_unrequestedCode_absent() {
        Discount d = disc(1L, 1, new BigDecimal("20"), StackingPolicy.BEST_WINS);

        Map<String, List<Discount>> result = build(List.of(d))
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-OTHER"), null, null, null));

        assertEquals(0, result.size());
    }
}
