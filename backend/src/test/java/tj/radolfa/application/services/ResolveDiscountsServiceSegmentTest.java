package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.ExpandCategoryTargetPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadUserSegmentContextPort;
import tj.radolfa.application.ports.out.LoadUserSegmentContextPort.UserSegmentContext;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.Segment;
import tj.radolfa.domain.model.SegmentTarget;
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

class ResolveDiscountsServiceSegmentTest {

    // ---- Fakes ----

    record FakeSegmentCtx(UserSegmentContext ctx) implements LoadUserSegmentContextPort {
        @Override public Optional<UserSegmentContext> loadFor(Long userId) { return Optional.ofNullable(ctx); }
    }

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        private final List<Discount> nonSkuTargeted;

        FakeLoadDiscountPort(List<Discount> nonSkuTargeted) {
            this.nonSkuTargeted = nonSkuTargeted;
        }

        @Override public List<Discount> findActiveByItemCodes(Collection<String> c) { return List.of(); }
        @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return nonSkuTargeted; }
        @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
        @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
        @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
    }

    static final ExpandCategoryTargetPort NO_EXPAND = cats -> List.of();
    static final QueryDiscountUsagePort NO_USAGE = new QueryDiscountUsagePort() {
        @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return Map.of(); }
        @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long userId) { return Map.of(); }
    };

    // ---- Fixture helpers ----

    static DiscountType bwType() {
        return new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);
    }

    static Discount tierDiscount(Long tierId) {
        return new Discount(1L, bwType(),
                List.of(new SkuTarget("SKU-T"), new SegmentTarget(Segment.LOYALTY_TIER, tierId.toString())),
                AmountType.PERCENT, new BigDecimal("15"),
                Instant.EPOCH, Instant.MAX, false, "Tier disc", "#000", null, null, null, null);
    }

    static Discount newCustomerDiscount() {
        return new Discount(2L, bwType(),
                List.of(new SkuTarget("SKU-N"), new SegmentTarget(Segment.NEW_CUSTOMER, null)),
                AmountType.PERCENT, new BigDecimal("10"),
                Instant.EPOCH, Instant.MAX, false, "New customer", "#111", null, null, null, null);
    }

    ResolveDiscountsService build(Discount d, UserSegmentContext ctx) {
        // SKU target handled by skuTargeted path; segment filter applied on top
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return d.itemCodes().stream().anyMatch(codes::contains) ? List.of(d) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };
        return new ResolveDiscountsService(port, NO_EXPAND, NO_USAGE, new FakeSegmentCtx(ctx));
    }

    // ---- Tests ----

    @Test
    @DisplayName("LOYALTY_TIER: user on matching tier sees the discount")
    void loyaltyTier_matchingTier_discountApplies() {
        Discount d = tierDiscount(5L);
        ResolveDiscountsService service = build(d,
                new UserSegmentContext(1L, 5L, false));

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-T"), 1L, null));

        assertTrue(result.containsKey("SKU-T"));
    }

    @Test
    @DisplayName("LOYALTY_TIER: user on different tier does not see the discount")
    void loyaltyTier_wrongTier_discountFiltered() {
        Discount d = tierDiscount(5L);
        ResolveDiscountsService service = build(d,
                new UserSegmentContext(1L, 7L, false)); // tier=7, not 5

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-T"), 1L, null));

        assertFalse(result.containsKey("SKU-T"));
    }

    @Test
    @DisplayName("LOYALTY_TIER: guest (null userId) never sees tier-gated discount")
    void loyaltyTier_guest_hidden() {
        Discount d = tierDiscount(5L);
        // Guest path — userId=null, no segment context loaded
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains("SKU-T") ? List.of(d) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };
        ResolveDiscountsService service = new ResolveDiscountsService(
                port, NO_EXPAND, NO_USAGE, new FakeSegmentCtx(null));

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-T"), null, null));

        assertFalse(result.containsKey("SKU-T"));
    }

    @Test
    @DisplayName("NEW_CUSTOMER: user with 0 orders sees the discount")
    void newCustomer_zeroOrders_discountApplies() {
        Discount d = newCustomerDiscount();
        ResolveDiscountsService service = build(d,
                new UserSegmentContext(2L, null, true)); // isNewCustomer=true

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-N"), 2L, null));

        assertTrue(result.containsKey("SKU-N"));
    }

    @Test
    @DisplayName("NEW_CUSTOMER: user with orders does not see the discount")
    void newCustomer_hasOrders_discountFiltered() {
        Discount d = newCustomerDiscount();
        ResolveDiscountsService service = build(d,
                new UserSegmentContext(2L, null, false)); // isNewCustomer=false

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-N"), 2L, null));

        assertFalse(result.containsKey("SKU-N"));
    }

    @Test
    @DisplayName("NEW_CUSTOMER: guest (userId=null) sees the discount — guest treated as new customer")
    void newCustomer_guest_discountApplied() {
        Discount d = newCustomerDiscount();
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains("SKU-N") ? List.of(d) : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };
        ResolveDiscountsService service = new ResolveDiscountsService(
                port, NO_EXPAND, NO_USAGE, new FakeSegmentCtx(null));

        Map<String, List<Discount>> result = service.resolve(
                new ResolveDiscountsUseCase.Query(List.of("SKU-N"), null, null));

        assertTrue(result.containsKey("SKU-N"));
    }
}
