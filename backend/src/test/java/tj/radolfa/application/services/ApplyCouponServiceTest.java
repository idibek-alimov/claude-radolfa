package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.cart.ApplyCouponUseCase;
import tj.radolfa.application.ports.in.cart.GetCartUseCase;
import tj.radolfa.application.ports.in.discount.ResolveDiscountsUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.readmodel.CartView;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Cart;
import tj.radolfa.domain.model.CartStatus;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ApplyCouponServiceTest {

    static final Long   SKU_ID  = 1L;
    static final String SKU_CODE = "SKU-X";
    static final String COUPON  = "PROMO20";

    // ---- Fixtures ----

    static Cart activeCart() {
        Cart cart = new Cart(10L, 99L, CartStatus.ACTIVE, List.of(), Instant.now(), Instant.now(), null);
        cart.addItem(SKU_ID, 2, new Money(new BigDecimal("500")));
        return cart;
    }

    static Discount couponDiscount(boolean disabled, Instant from, Instant upto,
                                    Integer cap, String code) {
        DiscountType type = new DiscountType(1L, "T", 1, StackingPolicy.BEST_WINS);
        return new Discount(5L, type,
                List.of(new SkuTarget(SKU_CODE)),
                AmountType.PERCENT, new BigDecimal("20"),
                from, upto, disabled, "Coupon Disc", "#000",
                null, cap, null, code);
    }

    static Discount validDiscount() {
        return couponDiscount(false, Instant.EPOCH, Instant.MAX, null, COUPON);
    }

    // ---- Fakes ----

    static LoadSkuPort fakeSkuPort() {
        return new LoadSkuPort() {
            @Override public List<Sku> findAllByIds(Collection<Long> ids) {
                return ids.contains(SKU_ID)
                        ? List.of(new Sku(SKU_ID, 1L, SKU_CODE, "M", 10, new Money(new BigDecimal("500"))))
                        : List.of();
            }
            @Override public Optional<Sku> findSkuById(Long id) { return Optional.empty(); }
            @Override public Optional<Sku> findBySkuCode(String c) { return Optional.empty(); }
            @Override public List<Sku> findSkusByVariantId(Long id) { return List.of(); }
        };
    }

    static CartView stubCartView() {
        return new CartView(10L, List.of(), new Money(new BigDecimal("1000")), 2, COUPON);
    }

    static GetCartUseCase fakeGetCart() {
        return userId -> stubCartView();
    }

    static SaveCartPort fakeSave() {
        return cart -> cart;
    }

    ApplyCouponService build(Discount storedDiscount,
                              Cart activeCart,
                              Map<Long, Long> usageCounts,
                              Map<String, List<Discount>> resolverResult) {
        LoadDiscountPort discountPort = new LoadDiscountPort() {
            @Override public Optional<Discount> findByCouponCode(String code) {
                if (storedDiscount != null && storedDiscount.couponCode() != null
                        && storedDiscount.couponCode().equalsIgnoreCase(code)) {
                    return Optional.of(storedDiscount);
                }
                return Optional.empty();
            }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public List<Discount> findActiveByItemCodes(Collection<String> c) { return List.of(); }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };

        LoadCartPort cartPort = new LoadCartPort() {
            @Override public Optional<Cart> findActiveByUserId(Long userId) {
                return Optional.ofNullable(activeCart);
            }
            @Override public Optional<Cart> findById(Long id) { return Optional.empty(); }
        };

        QueryDiscountUsagePort usagePort = new QueryDiscountUsagePort() {
            @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return usageCounts; }
            @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long u) { return Map.of(); }
        };

        ResolveDiscountsUseCase resolver = q -> resolverResult;

        return new ApplyCouponService(discountPort, cartPort, fakeSave(),
                fakeSkuPort(), resolver, usagePort, fakeGetCart());
    }

    // ---- Tests ----

    @Test
    @DisplayName("Valid coupon: result is valid, affected SKUs populated, cart view returned")
    void validCoupon_successResult() {
        Discount d = validDiscount();
        Map<String, List<Discount>> resolved = Map.of(SKU_CODE, List.of(d));

        ApplyCouponUseCase.Result r = build(d, activeCart(), Map.of(), resolved)
                .execute(99L, COUPON);

        assertTrue(r.valid());
        assertEquals(5L, r.discountId());
        assertEquals(List.of(SKU_CODE), r.affectedSkus());
        assertNull(r.invalidReason());
        assertNotNull(r.cart());
    }

    @Test
    @DisplayName("Unknown coupon code → NOT_FOUND")
    void unknownCode_notFound() {
        ApplyCouponUseCase.Result r = build(null, activeCart(), Map.of(), Map.of())
                .execute(99L, "UNKNOWN");

        assertFalse(r.valid());
        assertEquals("NOT_FOUND", r.invalidReason());
    }

    @Test
    @DisplayName("Disabled discount → DISABLED")
    void disabledDiscount_disabled() {
        Discount d = couponDiscount(true, Instant.EPOCH, Instant.MAX, null, COUPON);
        ApplyCouponUseCase.Result r = build(d, activeCart(), Map.of(), Map.of())
                .execute(99L, COUPON);

        assertFalse(r.valid());
        assertEquals("DISABLED", r.invalidReason());
    }

    @Test
    @DisplayName("Expired discount → NOT_ACTIVE")
    void expiredDiscount_notActive() {
        Discount d = couponDiscount(false, Instant.EPOCH, Instant.EPOCH, null, COUPON);
        ApplyCouponUseCase.Result r = build(d, activeCart(), Map.of(), Map.of())
                .execute(99L, COUPON);

        assertFalse(r.valid());
        assertEquals("NOT_ACTIVE", r.invalidReason());
    }

    @Test
    @DisplayName("Cap exhausted (usageCapTotal=1, 1 use) → CAP_EXHAUSTED")
    void capExhausted_capExhausted() {
        Discount d = couponDiscount(false, Instant.EPOCH, Instant.MAX, 1, COUPON);
        ApplyCouponUseCase.Result r = build(d, activeCart(), Map.of(5L, 1L), Map.of())
                .execute(99L, COUPON);

        assertFalse(r.valid());
        assertEquals("CAP_EXHAUSTED", r.invalidReason());
    }

    @Test
    @DisplayName("Coupon valid but no cart items match the discount targets → NO_ITEMS_AFFECTED")
    void noItemsAffected_noItemsAffected() {
        Discount d = validDiscount();
        // Resolver returns empty map — discount doesn't apply to any cart SKU
        ApplyCouponUseCase.Result r = build(d, activeCart(), Map.of(), Map.of())
                .execute(99L, COUPON);

        assertFalse(r.valid());
        assertEquals("NO_ITEMS_AFFECTED", r.invalidReason());
    }

    @Test
    @DisplayName("No active cart → throws IllegalStateException")
    void noActiveCart_throws() {
        assertThrows(IllegalStateException.class, () ->
                build(validDiscount(), null, Map.of(), Map.of()).execute(99L, COUPON));
    }
}
