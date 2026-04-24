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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveDiscountsServiceMinBasketTest {

    // ---- Fakes ----

    static final ExpandCategoryTargetPort NO_EXPAND = cats -> List.of();
    static final LoadUserSegmentContextPort NO_SEGMENT = userId -> Optional.empty();
    static final QueryDiscountUsagePort NO_USAGE = new QueryDiscountUsagePort() {
        @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return Map.of(); }
        @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long u) { return Map.of(); }
    };

    // ---- Fixture helpers ----

    static Discount discountWithMinBasket(BigDecimal minBasket) {
        DiscountType type = new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);
        return new Discount(1L, type,
                List.of(new SkuTarget("SKU-A")),
                AmountType.PERCENT, new BigDecimal("10"),
                Instant.EPOCH, Instant.MAX, false,
                "Min basket disc", "#000", minBasket, null, null, null);
    }

    ResolveDiscountsService build(Discount d) {
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains("SKU-A") ? List.of(d) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };
        return new ResolveDiscountsService(port, NO_EXPAND, NO_USAGE, NO_SEGMENT);
    }

    // ---- Tests ----

    @Test
    @DisplayName("cartSubtotal below minBasketAmount → discount filtered")
    void subtotalBelowMin_filtered() {
        Discount d = discountWithMinBasket(new BigDecimal("500"));
        Map<String, List<Discount>> result = build(d).resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, new BigDecimal("400")));

        assertFalse(result.containsKey("SKU-A"));
    }

    @Test
    @DisplayName("cartSubtotal exactly at minBasketAmount → discount included")
    void subtotalAtMin_included() {
        Discount d = discountWithMinBasket(new BigDecimal("500"));
        Map<String, List<Discount>> result = build(d).resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, new BigDecimal("500")));

        assertTrue(result.containsKey("SKU-A"));
    }

    @Test
    @DisplayName("cartSubtotal above minBasketAmount → discount included")
    void subtotalAboveMin_included() {
        Discount d = discountWithMinBasket(new BigDecimal("500"));
        Map<String, List<Discount>> result = build(d).resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, new BigDecimal("600")));

        assertTrue(result.containsKey("SKU-A"));
    }

    @Test
    @DisplayName("cartSubtotal=null (listing time) → discount included optimistically")
    void subtotalNull_listingTime_included() {
        Discount d = discountWithMinBasket(new BigDecimal("500"));
        Map<String, List<Discount>> result = build(d).resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null));

        assertTrue(result.containsKey("SKU-A"));
    }

    @Test
    @DisplayName("No minBasketAmount set → always included regardless of subtotal")
    void noMinBasket_alwaysIncluded() {
        Discount d = discountWithMinBasket(null);
        Map<String, List<Discount>> result = build(d).resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, new BigDecimal("1")));

        assertTrue(result.containsKey("SKU-A"));
    }
}
