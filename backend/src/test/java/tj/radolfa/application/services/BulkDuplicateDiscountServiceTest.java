package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.discount.BulkDuplicateDiscountUseCase;
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

class BulkDuplicateDiscountServiceTest {

    private FakeLoadDiscountPort fakeLoad;
    private FakeSaveDiscountPort fakeSave;
    private BulkDuplicateDiscountService service;

    private static final DiscountType FLASH = new DiscountType(1L, "FLASH_SALE", 1, StackingPolicy.BEST_WINS);
    private static final Instant FROM = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant UPTO = Instant.parse("2099-12-31T00:00:00Z");

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadDiscountPort();
        fakeSave = new FakeSaveDiscountPort();
        service  = new BulkDuplicateDiscountService(fakeLoad, fakeSave);
    }

    @Test
    @DisplayName("Duplicates have null id passed to save, are disabled, and title prefixed 'Copy of '")
    void execute_createsCopiesWithCorrectDefaults() {
        Discount original = discount(1L, "Summer Sale", false);
        fakeLoad.store(original);

        List<Discount> result = service.execute(new BulkDuplicateDiscountUseCase.Command(List.of(1L)));

        assertEquals(1, result.size());

        // Verify save was called with id=null
        assertEquals(1, fakeSave.savedWithNullId.size());

        Discount saved = result.get(0);
        assertNotNull(saved.id(), "Saved copy must have an assigned id");
        assertTrue(saved.disabled(), "Copy must be disabled");
        assertEquals("Copy of Summer Sale", saved.title());
        assertEquals(original.itemCodes(), saved.itemCodes());
        assertEquals(original.amountValue(), saved.amountValue());
        assertEquals(original.validFrom(), saved.validFrom());
        assertEquals(original.validUpto(), saved.validUpto());
        assertEquals(original.colorHex(), saved.colorHex());
    }

    @Test
    @DisplayName("Multiple originals produce multiple copies in order")
    void execute_multipleIds_produceMultipleCopies() {
        fakeLoad.store(discount(1L, "Camp A", false), discount(2L, "Camp B", true));

        List<Discount> result = service.execute(
                new BulkDuplicateDiscountUseCase.Command(List.of(1L, 2L)));

        assertEquals(2, result.size());
        assertEquals("Copy of Camp A", result.get(0).title());
        assertEquals("Copy of Camp B", result.get(1).title());
    }

    @Test
    @DisplayName("Non-existent ids are silently skipped")
    void execute_nonExistentId_skipped() {
        fakeLoad.store(discount(1L, "Camp", false));

        List<Discount> result = service.execute(
                new BulkDuplicateDiscountUseCase.Command(List.of(1L, 999L)));

        assertEquals(1, result.size());
    }

    // ---- Helpers ----

    private static Discount discount(Long id, String title, boolean disabled) {
        List<DiscountTarget> targets = new ArrayList<>(List.of(new SkuTarget("SKU-" + id)));
        return new Discount(id, FLASH, targets, AmountType.PERCENT,
                new BigDecimal("15.00"), FROM, UPTO, disabled, title, "#AABBCC", null, null, null, null);
    }

    // ---- Fakes ----

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        private final Map<Long, Discount> store = new HashMap<>();

        void store(Discount... discounts) {
            for (Discount d : discounts) store.put(d.id(), d);
        }

        @Override public Optional<Discount> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
        @Override public List<Discount> findActiveByItemCodes(Collection<String> c) { return List.of(); }
        @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
        @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) {
            return new PageImpl<>(List.copyOf(store.values()), p, store.size());
        }
        @Override public Optional<Discount> findByCouponCode(String code) { return Optional.empty(); }
    }

    static class FakeSaveDiscountPort implements SaveDiscountPort {
        final List<Discount> savedWithNullId = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(200);

        @Override
        public Discount save(Discount d) {
            if (d.id() == null) savedWithNullId.add(d);
            return new Discount(
                    d.id() != null ? d.id() : idGen.getAndIncrement(),
                    d.type(), d.targets(), d.amountType(), d.amountValue(),
                    d.validFrom(), d.validUpto(), d.disabled(), d.title(), d.colorHex(),
                    d.minBasketAmount(), d.usageCapTotal(), d.usageCapPerCustomer(), d.couponCode()
            );
        }

        @Override public void delete(Long id) {}
    }
}
