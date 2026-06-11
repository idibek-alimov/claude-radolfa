package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.in.discount.BulkDeleteDiscountUseCase;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BulkDeleteDiscountServiceTest {

    private FakeLoadDiscountPort fakeLoad;
    private FakeSaveDiscountPort fakeSave;
    private BulkDeleteDiscountService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadDiscountPort();
        fakeSave = new FakeSaveDiscountPort();
        service  = new BulkDeleteDiscountService(fakeLoad, fakeSave, event -> {});
    }

    @Test
    @DisplayName("Deletes each id and returns total count")
    void execute_deletesAll_returnsCount() {
        int affected = service.execute(new BulkDeleteDiscountUseCase.Command(List.of(1L, 2L, 3L)));

        assertEquals(3, affected);
        assertEquals(List.of(1L, 2L, 3L), fakeSave.deletedIds);
    }

    @Test
    @DisplayName("Empty id list returns zero and makes no delete calls")
    void execute_emptyList_noDeletes() {
        int affected = service.execute(new BulkDeleteDiscountUseCase.Command(List.of()));

        assertEquals(0, affected);
        assertTrue(fakeSave.deletedIds.isEmpty());
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
        final List<Long> deletedIds = new ArrayList<>();

        @Override public Discount save(Discount d) { return d; }

        @Override
        public void delete(Long id) { deletedIds.add(id); }
    }
}
