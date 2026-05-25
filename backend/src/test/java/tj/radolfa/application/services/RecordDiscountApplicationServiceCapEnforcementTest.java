package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.RecordDiscountApplicationUseCase;
import tj.radolfa.application.ports.out.LockDiscountForUsagePort;
import tj.radolfa.application.ports.out.QueryDiscountUsagePort;
import tj.radolfa.application.ports.out.SaveDiscountApplicationPort;
import tj.radolfa.domain.exception.DiscountUsageCapExceededException;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountApplication;
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

import static org.junit.jupiter.api.Assertions.*;

class RecordDiscountApplicationServiceCapEnforcementTest {

    // ---- Helpers ----

    static final DiscountType TYPE = new DiscountType(1L, "SALE", 1, StackingPolicy.BEST_WINS);

    static Discount discount(Long id, Integer capTotal, Integer capPerCustomer) {
        return new Discount(id, TYPE, List.of(new SkuTarget("SKU-X")),
                AmountType.PERCENT, BigDecimal.TEN,
                Instant.EPOCH, Instant.MAX, false, "Test", "#000",
                null, capTotal, capPerCustomer, null);
    }

    static LockDiscountForUsagePort lockReturning(Discount d) {
        return discountId -> d.id().equals(discountId) ? Optional.of(d) : Optional.empty();
    }

    static LockDiscountForUsagePort lockEmpty() {
        return discountId -> Optional.empty();
    }

    static QueryDiscountUsagePort usageWith(Map<Long, Long> totals, Map<Long, Long> perUser) {
        return new QueryDiscountUsagePort() {
            @Override public Map<Long, Long> countByDiscountIds(Collection<Long> ids) { return totals; }
            @Override public Map<Long, Long> countByDiscountIdsForUser(Collection<Long> ids, Long userId) { return perUser; }
        };
    }

    static class CapturingSavePort implements SaveDiscountApplicationPort {
        private final List<DiscountApplication> saved = new ArrayList<>();
        @Override public DiscountApplication save(DiscountApplication app) { saved.add(app); return app; }
        boolean wasCalled() { return !saved.isEmpty(); }
    }

    static RecordDiscountApplicationService build(LockDiscountForUsagePort lock,
                                                   QueryDiscountUsagePort usage,
                                                   SaveDiscountApplicationPort save) {
        return new RecordDiscountApplicationService(lock, usage, save);
    }

    static RecordDiscountApplicationUseCase.Command cmd(Long discountId, Long userId) {
        return new RecordDiscountApplicationUseCase.Command(
                discountId, 1L, 2L, "SKU-X", 1,
                new BigDecimal("100.00"), new BigDecimal("90.00"), userId);
    }

    // ---- Tests ----

    @Test
    @DisplayName("total cap not yet reached → row is saved")
    void totalCap_notReached_saves() {
        Discount d = discount(1L, 5, null);
        CapturingSavePort save = new CapturingSavePort();

        build(lockReturning(d), usageWith(Map.of(1L, 4L), Map.of()), save)
                .execute(cmd(1L, null));

        assertTrue(save.wasCalled());
    }

    @Test
    @DisplayName("total cap exactly reached → DiscountUsageCapExceededException(TOTAL) thrown, save not called")
    void totalCap_reached_throws() {
        Discount d = discount(2L, 5, null);
        CapturingSavePort save = new CapturingSavePort();

        DiscountUsageCapExceededException ex = assertThrows(
                DiscountUsageCapExceededException.class,
                () -> build(lockReturning(d), usageWith(Map.of(2L, 5L), Map.of()), save)
                        .execute(cmd(2L, null)));

        assertEquals(2L, ex.getDiscountId());
        assertEquals(DiscountUsageCapExceededException.Scope.TOTAL, ex.getScope());
        assertFalse(save.wasCalled());
    }

    @Test
    @DisplayName("per-customer cap reached for given user → DiscountUsageCapExceededException(PER_CUSTOMER) thrown")
    void perCustomerCap_reached_throws() {
        Discount d = discount(3L, null, 2);
        CapturingSavePort save = new CapturingSavePort();

        DiscountUsageCapExceededException ex = assertThrows(
                DiscountUsageCapExceededException.class,
                () -> build(lockReturning(d), usageWith(Map.of(), Map.of(3L, 2L)), save)
                        .execute(cmd(3L, 99L)));

        assertEquals(3L, ex.getDiscountId());
        assertEquals(DiscountUsageCapExceededException.Scope.PER_CUSTOMER, ex.getScope());
        assertFalse(save.wasCalled());
    }

    @Test
    @DisplayName("per-customer cap reached but no userId supplied → total-only enforcement, row saved")
    void perCustomerCap_noUserId_savesAnyway() {
        Discount d = discount(4L, null, 2);
        CapturingSavePort save = new CapturingSavePort();

        build(lockReturning(d), usageWith(Map.of(), Map.of(4L, 2L)), save)
                .execute(cmd(4L, null)); // no userId → per-customer check skipped

        assertTrue(save.wasCalled());
    }

    @Test
    @DisplayName("both caps null → row is saved without cap queries being necessary")
    void noCaps_alwaysSaves() {
        Discount d = discount(5L, null, null);
        CapturingSavePort save = new CapturingSavePort();

        build(lockReturning(d), usageWith(Map.of(), Map.of()), save)
                .execute(cmd(5L, 1L));

        assertTrue(save.wasCalled());
    }

    @Test
    @DisplayName("discount not found via lock port → IllegalStateException, save not called")
    void discountNotFound_throws() {
        CapturingSavePort save = new CapturingSavePort();

        assertThrows(IllegalStateException.class,
                () -> build(lockEmpty(), usageWith(Map.of(), Map.of()), save)
                        .execute(cmd(99L, null)));

        assertFalse(save.wasCalled());
    }
}
