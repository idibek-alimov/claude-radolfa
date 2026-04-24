package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.discount.BulkToggleDiscountUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class BulkToggleDiscountServiceTest {

    private FakeLoadDiscountPort fakeLoad;
    private FakeSaveDiscountPort fakeSave;
    private BulkToggleDiscountService service;

    private static final DiscountType FLASH = new DiscountType(1L, "FLASH_SALE", 1, StackingPolicy.BEST_WINS);
    private static final Instant FROM = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant UPTO = Instant.parse("2099-12-31T00:00:00Z");

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadDiscountPort();
        fakeSave = new FakeSaveDiscountPort();
        service  = new BulkToggleDiscountService(fakeLoad, fakeSave);
    }

    @Test
    @DisplayName("Disables all given ids and returns count")
    void execute_disable_returnsAffectedCount() {
        fakeLoad.store(discount(1L, false), discount(2L, false));

        int affected = service.execute(new BulkToggleDiscountUseCase.Command(List.of(1L, 2L), true));

        assertEquals(2, affected);
        assertEquals(2, fakeSave.savedDiscounts.size());
        assertTrue(fakeSave.savedDiscounts.stream().allMatch(Discount::disabled));
    }

    @Test
    @DisplayName("Enables all given ids and returns count")
    void execute_enable_setsDisabledFalse() {
        fakeLoad.store(discount(1L, true), discount(2L, true));

        int affected = service.execute(new BulkToggleDiscountUseCase.Command(List.of(1L, 2L), false));

        assertEquals(2, affected);
        assertTrue(fakeSave.savedDiscounts.stream().noneMatch(Discount::disabled));
    }

    @Test
    @DisplayName("Non-existent ids are silently skipped; affected count reflects only found ids")
    void execute_nonExistentIds_skipped() {
        fakeLoad.store(discount(1L, false));

        int affected = service.execute(
                new BulkToggleDiscountUseCase.Command(List.of(1L, 999L), true));

        assertEquals(1, affected);
        assertEquals(1, fakeSave.savedDiscounts.size());
    }

    @Test
    @DisplayName("Empty id list returns zero affected")
    void execute_emptyIds_returnsZero() {
        int affected = service.execute(new BulkToggleDiscountUseCase.Command(List.of(), false));
        assertEquals(0, affected);
    }

    // ---- Helpers ----

    private static Discount discount(Long id, boolean disabled) {
        List<DiscountTarget> targets = List.of(new SkuTarget("SKU-" + id));
        return new Discount(id, FLASH, targets, AmountType.PERCENT, new BigDecimal("10.00"),
                FROM, UPTO, disabled, "Camp-" + id, "#FFFFFF", null, null, null, null);
    }

    // ---- Fakes ----

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        private final Map<Long, Discount> store = new HashMap<>();

        void store(Discount... discounts) {
            for (Discount d : discounts) store.put(d.id(), d);
        }

        @Override
        public Optional<Discount> findById(Long id) { return Optional.ofNullable(store.get(id)); }

        @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
        @Override public List<Discount> findActiveByItemCodes(Collection<String> c) { return List.of(); }
        @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
        @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) {
            return new PageImpl<>(List.copyOf(store.values()), p, store.size());
        }
    }

    static class FakeSaveDiscountPort implements SaveDiscountPort {
        final List<Discount> savedDiscounts = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(100);

        @Override
        public Discount save(Discount d) {
            Discount persisted = d.id() != null ? d
                    : new Discount(idGen.getAndIncrement(), d.type(), d.targets(),
                    d.amountType(), d.amountValue(), d.validFrom(), d.validUpto(),
                    d.disabled(), d.title(), d.colorHex(),
                    d.minBasketAmount(), d.usageCapTotal(), d.usageCapPerCustomer(), d.couponCode());
            savedDiscounts.add(persisted);
            return persisted;
        }

        @Override
        public void delete(Long id) {}
    }
}
