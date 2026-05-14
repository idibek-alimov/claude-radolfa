package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CheckCouponAvailabilityServiceTest {

    static Discount discountWithCoupon(Long id, String couponCode) {
        DiscountType type = new DiscountType(1L, "TYPE-1", 1, StackingPolicy.BEST_WINS);
        return new Discount(id, type,
                List.of(new SkuTarget("SKU-A")),
                AmountType.PERCENT, new BigDecimal("10"),
                Instant.EPOCH, Instant.MAX, false, "D" + id, "#000",
                null, null, null, couponCode);
    }

    CheckCouponAvailabilityService build(Discount stored) {
        LoadDiscountPort port = new LoadDiscountPort() {
            @Override public Optional<Discount> findByCouponCode(String code) {
                if (stored != null && stored.couponCode() != null
                        && stored.couponCode().equalsIgnoreCase(code)) {
                    return Optional.of(stored);
                }
                return Optional.empty();
            }
            @Override public Optional<Discount> findById(Long id) { return Optional.empty(); }
            @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
            @Override public List<Discount> findActiveByItemCodes(Collection<String> c) { return List.of(); }
            @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
            @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) { return Page.empty(); }
        };
        return new CheckCouponAvailabilityService(port);
    }

    @Test
    @DisplayName("Code not used by any discount → available")
    void codeAbsent_available() {
        CheckCouponAvailabilityService svc = build(null);
        assertTrue(svc.isAvailable("PROMO10", null));
    }

    @Test
    @DisplayName("Code already used by another discount → unavailable")
    void codePresent_noExclusion_unavailable() {
        Discount existing = discountWithCoupon(5L, "PROMO10");
        CheckCouponAvailabilityService svc = build(existing);
        assertFalse(svc.isAvailable("PROMO10", null));
    }

    @Test
    @DisplayName("Code present, but excludeId matches that discount → available (edit flow)")
    void codePresent_excludeIdMatches_available() {
        Discount existing = discountWithCoupon(5L, "PROMO10");
        CheckCouponAvailabilityService svc = build(existing);
        assertTrue(svc.isAvailable("PROMO10", 5L));
    }

    @Test
    @DisplayName("Code present, excludeId given but does not match → unavailable")
    void codePresent_excludeIdMismatch_unavailable() {
        Discount existing = discountWithCoupon(5L, "PROMO10");
        CheckCouponAvailabilityService svc = build(existing);
        assertFalse(svc.isAvailable("PROMO10", 99L));
    }
}
