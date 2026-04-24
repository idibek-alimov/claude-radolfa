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

class ResolveDiscountsServiceUsageCapTest {

    // ---- Fakes ----

    static final ExpandCategoryTargetPort NO_EXPAND = cats -> List.of();
    static final LoadUserSegmentContextPort NO_SEGMENT = userId -> Optional.empty();

    static Discount skuDiscount(Long id, String code, Integer capTotal, Integer capPerCustomer) {
        DiscountType type = new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);
        return new Discount(id, type,
                List.of(new SkuTarget(code)),
                AmountType.PERCENT, new BigDecimal("10"),
                Instant.EPOCH, Instant.MAX, false,
                "Test disc", "#000", null, capTotal, capPerCustomer, null);
    }

    ResolveDiscountsService build(Discount d,
                                   Map<Long, Long> totalUsage,
                                   Map<Long, Long> userUsage) {
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return d.itemCodes().stream().anyMatch(codes::contains) ? List.of(d) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };
        QueryDiscountUsagePort usagePort = new QueryDiscountUsagePort() {
            @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return totalUsage; }
            @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long u) { return userUsage; }
        };
        return new ResolveDiscountsService(port, NO_EXPAND, usagePort, NO_SEGMENT);
    }

    // ---- Tests ----

    @Test
    @DisplayName("usageCapTotal: count equals cap → discount filtered out")
    void usageCapTotal_exhausted_filtered() {
        Discount d = skuDiscount(1L, "SKU-A", 2, null);
        ResolveDiscountsService service = build(d, Map.of(1L, 2L), Map.of());

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-A"), 99L, null));

        assertFalse(result.containsKey("SKU-A"));
    }

    @Test
    @DisplayName("usageCapTotal: count below cap → discount included")
    void usageCapTotal_notExhausted_included() {
        Discount d = skuDiscount(1L, "SKU-A", 2, null);
        ResolveDiscountsService service = build(d, Map.of(1L, 1L), Map.of());

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-A"), 99L, null));

        assertTrue(result.containsKey("SKU-A"));
    }

    @Test
    @DisplayName("usageCapPerCustomer: user exhausted their per-customer cap → filtered")
    void usageCapPerCustomer_userExhausted_filtered() {
        Discount d = skuDiscount(1L, "SKU-B", null, 1);
        ResolveDiscountsService service = build(d, Map.of(), Map.of(1L, 1L));

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-B"), 5L, null));

        assertFalse(result.containsKey("SKU-B"));
    }

    @Test
    @DisplayName("usageCapPerCustomer: different user has not used cap → included")
    void usageCapPerCustomer_differentUserUnaffected() {
        Discount d = skuDiscount(1L, "SKU-B", null, 1);
        // user 5 exhausted, but user 6 hasn't
        ResolveDiscountsService service = build(d, Map.of(), Map.of());  // empty per-user map for user 6

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-B"), 6L, null));

        assertTrue(result.containsKey("SKU-B"));
    }

    @Test
    @DisplayName("usageCapPerCustomer: guest (userId=null) bypasses per-customer cap")
    void usageCapPerCustomer_guest_bypassed() {
        Discount d = skuDiscount(1L, "SKU-C", null, 1);
        // Even with total usage present, per-customer is irrelevant for guests
        ResolveDiscountsService service = build(d, Map.of(1L, 0L), Map.of(1L, 999L));

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-C"), null, null));

        // Guest: per-user map is not consulted (userId=null branch)
        assertTrue(result.containsKey("SKU-C"));
    }

    @Test
    @DisplayName("No usage cap set → always included regardless of usage count")
    void noCapSet_alwaysIncluded() {
        Discount d = skuDiscount(1L, "SKU-D", null, null);
        ResolveDiscountsService service = build(d, Map.of(1L, 9999L), Map.of(1L, 9999L));

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-D"), 1L, null));

        assertTrue(result.containsKey("SKU-D"));
    }
}
