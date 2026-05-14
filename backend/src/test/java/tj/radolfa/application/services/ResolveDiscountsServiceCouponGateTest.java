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

import static org.junit.jupiter.api.Assertions.*;

class ResolveDiscountsServiceCouponGateTest {

    static final ExpandCategoryTargetPort NO_EXPAND = cats -> List.of();
    static final LoadUserSegmentContextPort NO_SEGMENT = userId -> Optional.empty();
    static final QueryDiscountUsagePort NO_USAGE = new QueryDiscountUsagePort() {
        @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return Map.of(); }
        @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long u) { return Map.of(); }
    };

    static Discount couponDiscount(String couponCode) {
        DiscountType type = new DiscountType(1L, "TYPE-1", 1, StackingPolicy.BEST_WINS);
        return new Discount(10L, type,
                List.of(new SkuTarget("SKU-A")),
                AmountType.PERCENT, new BigDecimal("20"),
                Instant.EPOCH, Instant.MAX, false, "Coupon Disc", "#000",
                null, null, null, couponCode);
    }

    static Discount publicDiscount() {
        DiscountType type = new DiscountType(2L, "TYPE-2", 2, StackingPolicy.BEST_WINS);
        return new Discount(20L, type,
                List.of(new SkuTarget("SKU-A")),
                AmountType.PERCENT, new BigDecimal("10"),
                Instant.EPOCH, Instant.MAX, false, "Public Disc", "#111",
                null, null, null, null);
    }

    ResolveDiscountsService build(Discount... discounts) {
        List<Discount> all = List.of(discounts);
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public List<Discount> findActiveByItemCodes(Collection<String> codes) {
                return codes.contains("SKU-A") ? all : List.of();
            }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
            @Override public Optional<Discount> findByCouponCode(String code) { return Optional.empty(); }
        };
        return new ResolveDiscountsService(port, NO_EXPAND, NO_USAGE, NO_SEGMENT);
    }

    @Test
    @DisplayName("Coupon-gated discount hidden when couponCode=null in query")
    void couponGated_hiddenWithNullCoupon() {
        Discount gated = couponDiscount("SUMMER25");
        Map<String, List<Discount>> result = build(gated)
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, null));

        assertFalse(result.containsKey("SKU-A"), "Coupon-gated discount must be invisible without coupon");
    }

    @Test
    @DisplayName("Coupon-gated discount revealed when correct code supplied")
    void couponGated_revealedWithCorrectCode() {
        Discount gated = couponDiscount("SUMMER25");
        Map<String, List<Discount>> result = build(gated)
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, "SUMMER25"));

        assertTrue(result.containsKey("SKU-A"));
        assertEquals(gated, result.get("SKU-A").get(0));
    }

    @Test
    @DisplayName("Wrong coupon code keeps the discount hidden")
    void couponGated_wrongCode_hidden() {
        Discount gated = couponDiscount("SUMMER25");
        Map<String, List<Discount>> result = build(gated)
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, "WINTER10"));

        assertFalse(result.containsKey("SKU-A"));
    }

    @Test
    @DisplayName("Coupon matching is case-insensitive: 'summer25' matches 'SUMMER25'")
    void couponGated_caseInsensitive() {
        Discount gated = couponDiscount("SUMMER25");
        Map<String, List<Discount>> result = build(gated)
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, "summer25"));

        assertTrue(result.containsKey("SKU-A"), "Case-insensitive match must work");
    }

    @Test
    @DisplayName("Non-coupon discount always passes the gate regardless of supplied code")
    void nonCoupon_alwaysPasses() {
        Discount pub = publicDiscount();
        Map<String, List<Discount>> result = build(pub)
                .resolve(new ResolveDiscountsUseCase.Query(List.of("SKU-A"), null, null, null));

        assertTrue(result.containsKey("SKU-A"), "Public discount must not be filtered by coupon gate");
    }
}
