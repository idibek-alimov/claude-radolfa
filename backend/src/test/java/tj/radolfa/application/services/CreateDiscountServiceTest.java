package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.discount.CreateDiscountUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.DiscountSnapshotPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.application.ports.out.SaveDiscountChangePort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.ChangeType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountChange;
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;
import tj.radolfa.domain.model.StackingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CreateDiscountServiceTest {

    private static final Long ACTOR_ID = 7L;
    private static final DiscountType FLASH = new DiscountType(1L, "FLASH_SALE", 1, StackingPolicy.BEST_WINS);
    private static final Instant FROM = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant UPTO = Instant.parse("2099-12-31T00:00:00Z");

    private FakeLoadDiscountTypePort fakeType;
    private FakeLoadDiscountPort fakeLoad;
    private FakeSaveDiscountPort fakeSave;
    private FakeSaveDiscountChangePort fakeChange;
    private CreateDiscountService service;

    @BeforeEach
    void setUp() {
        fakeType = new FakeLoadDiscountTypePort();
        fakeType.store(FLASH);
        fakeLoad = new FakeLoadDiscountPort();
        fakeSave = new FakeSaveDiscountPort();
        fakeChange = new FakeSaveDiscountChangePort();
        service = new CreateDiscountService(fakeType, fakeLoad, fakeSave, event -> {},
                fakeChange, new FakeDiscountSnapshotPort());
    }

    private static CreateDiscountUseCase.Command command() {
        return new CreateDiscountUseCase.Command(
                FLASH.id(), List.of(new SkuTarget("SKU-1")), AmountType.PERCENT, new BigDecimal("20.00"),
                FROM, UPTO, "Flash Sale", "#FF0000", null, null, null, null);
    }

    @Test
    @DisplayName("Creating a discount writes one CREATE ledger row with oldValueJson=null")
    void create_recordsLedgerRow_withNullOldValue() {
        Discount created = service.execute(command(), ACTOR_ID);

        assertEquals(1, fakeChange.saved.size());
        DiscountChange change = fakeChange.saved.get(0);
        assertEquals(created.id(), change.discountId());
        assertEquals(ChangeType.CREATE, change.changeType());
        assertNull(change.oldValueJson(), "no prior state exists on create");
        assertNotNull(change.newValueJson());
        assertEquals(ACTOR_ID, change.actorUserId());
        assertNotNull(change.occurredAt());
    }

    // ---- Fakes ----

    static class FakeLoadDiscountTypePort implements LoadDiscountTypePort {
        private final Map<Long, DiscountType> store = new HashMap<>();
        void store(DiscountType... types) { for (DiscountType t : types) store.put(t.id(), t); }
        @Override public List<DiscountType> findAll() { return List.copyOf(store.values()); }
        @Override public Optional<DiscountType> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public long countDiscountsByTypeId(Long typeId) { return 0; }
    }

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        private final Map<Long, Discount> store = new HashMap<>();
        void store(Discount... discounts) { for (Discount d : discounts) store.put(d.id(), d); }
        @Override public Optional<Discount> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Discount> findActiveByItemCode(String c) { return List.of(); }
        @Override public List<Discount> findActiveByItemCodes(java.util.Collection<String> c) { return List.of(); }
        @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
        @Override public Page<Discount> findAll(DiscountFilter f, Pageable p) {
            return new PageImpl<>(List.copyOf(store.values()), p, store.size());
        }
        @Override public Optional<Discount> findByCouponCode(String code) { return Optional.empty(); }
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

    static class FakeSaveDiscountChangePort implements SaveDiscountChangePort {
        final List<DiscountChange> saved = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public DiscountChange save(DiscountChange change) {
            DiscountChange withId = new DiscountChange(idGen.getAndIncrement(), change.discountId(),
                    change.changeType(), change.oldValueJson(), change.newValueJson(),
                    change.actorUserId(), change.occurredAt());
            saved.add(withId);
            return withId;
        }
    }

    /** Deterministic stub — real JSON-shape correctness is verified live, not in this unit test. */
    static class FakeDiscountSnapshotPort implements DiscountSnapshotPort {
        @Override
        public String toJson(Discount discount) {
            return "snap:" + discount.amountValue() + ":" + discount.disabled();
        }
    }
}
