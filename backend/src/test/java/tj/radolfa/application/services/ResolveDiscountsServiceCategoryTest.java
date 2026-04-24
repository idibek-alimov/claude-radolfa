package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.ExpandCategoryTargetPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadUserSegmentContextPort;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.CategoryTarget;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveDiscountsServiceCategoryTest {

    // ---- Fakes ----

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        private final List<Discount> nonSkuTargeted;

        FakeLoadDiscountPort(List<Discount> nonSkuTargeted) {
            this.nonSkuTargeted = nonSkuTargeted;
        }

        @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) { return List.of(); }
        @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return nonSkuTargeted; }
        @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
        @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
        @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
    }

    static class FakeExpandCategoryTargetPort implements ExpandCategoryTargetPort {
        private final Map<Long, List<String>> byCategory;

        FakeExpandCategoryTargetPort(Map<Long, List<String>> byCategory) {
            this.byCategory = byCategory;
        }

        @Override
        public List<String> resolveSkuCodes(Map<Long, Boolean> categoryToIncludeDescendants) {
            return categoryToIncludeDescendants.keySet().stream()
                    .flatMap(id -> byCategory.getOrDefault(id, List.of()).stream())
                    .distinct()
                    .toList();
        }
    }

    static class FakeQueryDiscountUsagePort implements QueryDiscountUsagePort {
        @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return Map.of(); }
        @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long userId) { return Map.of(); }
    }

    static class FakeLoadUserSegmentContextPort implements LoadUserSegmentContextPort {
        @Override public Optional<UserSegmentContext> loadFor(Long userId) { return Optional.empty(); }
    }

    // ---- Fixture helpers ----

    static Discount categoryDiscount(Long id, Long categoryId, boolean descendants, BigDecimal pct) {
        DiscountType type = new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);
        return new Discount(id, type,
                List.of(new CategoryTarget(categoryId, descendants)),
                AmountType.PERCENT, pct,
                Instant.EPOCH, Instant.MAX, false,
                "Cat discount", "#000", null, null, null, null);
    }

    ResolveDiscountsService buildService(List<Discount> nonSkuTargeted,
                                         Map<Long, List<String>> categoryToSkus) {
        return new ResolveDiscountsService(
                new FakeLoadDiscountPort(nonSkuTargeted),
                new FakeExpandCategoryTargetPort(categoryToSkus),
                new FakeQueryDiscountUsagePort(),
                new FakeLoadUserSegmentContextPort()
        );
    }

    // ---- Tests ----

    @Test
    @DisplayName("CategoryTarget with includeDescendants=true: only intersected item codes get the discount")
    void categoryTarget_descendantsTrue_onlyMatchingCodesReceiveDiscount() {
        Discount d = categoryDiscount(1L, 42L, true, new BigDecimal("20"));
        ResolveDiscountsService service = buildService(
                List.of(d),
                Map.of(42L, List.of("SKU-A", "SKU-B"))
        );

        Map<String, List<Discount>> result = service.resolve(
                new tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase.Query(
                        List.of("SKU-A", "SKU-C"), null, null));

        assertTrue(result.containsKey("SKU-A"), "SKU-A is in category, should be included");
        assertFalse(result.containsKey("SKU-B"), "SKU-B not requested, should be absent");
        assertFalse(result.containsKey("SKU-C"), "SKU-C not in category, should be excluded");
        assertEquals(1, result.get("SKU-A").size());
        assertEquals(d, result.get("SKU-A").get(0));
    }

    @Test
    @DisplayName("CategoryTarget with includeDescendants=false: uses same expansion path (no child categories injected)")
    void categoryTarget_descendantsFalse_usesExactCategorySkus() {
        Discount d = categoryDiscount(2L, 99L, false, new BigDecimal("10"));
        // Fake expansion for category 99 without descendants returns only SKU-X
        ResolveDiscountsService service = buildService(
                List.of(d),
                Map.of(99L, List.of("SKU-X"))
        );

        Map<String, List<Discount>> result = service.resolve(
                new tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase.Query(
                        List.of("SKU-X", "SKU-Y"), null, null));

        assertTrue(result.containsKey("SKU-X"));
        assertFalse(result.containsKey("SKU-Y"));
    }

    @Test
    @DisplayName("CategoryTarget on category with no products: no codes matched")
    void categoryTarget_emptyCategory_noResults() {
        Discount d = categoryDiscount(3L, 77L, true, new BigDecimal("15"));
        ResolveDiscountsService service = buildService(
                List.of(d),
                Map.of(77L, List.of())  // empty expansion
        );

        Map<String, List<Discount>> result = service.resolve(
                new tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase.Query(
                        List.of("SKU-A"), null, null));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("SKU-targeted and category-targeted discounts both apply to the same item code")
    void mixedTargets_bothApplyToSameCode() {
        DiscountType bwType = new DiscountType(1L, "SEASONAL", 2, StackingPolicy.BEST_WINS);
        Discount skuDisc = new Discount(10L, bwType,
                List.of(new SkuTarget("SKU-A")),
                AmountType.PERCENT, new BigDecimal("10"),
                Instant.EPOCH, Instant.MAX, false, "SKU disc", "#111", null, null, null, null);

        DiscountType catType = new DiscountType(2L, "FLASH", 1, StackingPolicy.BEST_WINS);
        Discount catDisc = new Discount(11L, catType,
                List.of(new CategoryTarget(5L, true)),
                AmountType.PERCENT, new BigDecimal("20"),
                Instant.EPOCH, Instant.MAX, false, "Cat disc", "#222", null, null, null, null);

        // SKU discount from SKU-targeted path; category discount from non-SKU path
        LoadDiscountPort loadPort = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains("SKU-A") ? List.of(skuDisc) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(catDisc); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };

        ResolveDiscountsService service = new ResolveDiscountsService(
                loadPort,
                new FakeExpandCategoryTargetPort(Map.of(5L, List.of("SKU-A"))),
                new FakeQueryDiscountUsagePort(),
                new FakeLoadUserSegmentContextPort()
        );

        Map<String, List<Discount>> result = service.resolve(
                new tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase.Query(
                        List.of("SKU-A"), null, null));

        // Both are BEST_WINS; catDisc has rank=1 (lower = higher priority), so it wins
        assertEquals(1, result.get("SKU-A").size(), "Only BEST_WINS winner should be returned");
        assertEquals(catDisc, result.get("SKU-A").get(0), "catDisc (rank=1) beats skuDisc (rank=2)");
    }
}
