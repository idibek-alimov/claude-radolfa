package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.BulkDeleteDiscountUseCase;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BulkDeleteDiscountServiceTest {

    private FakeSaveDiscountPort fakeSave;
    private BulkDeleteDiscountService service;

    @BeforeEach
    void setUp() {
        fakeSave = new FakeSaveDiscountPort();
        service  = new BulkDeleteDiscountService(fakeSave);
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

    // ---- Fake ----

    static class FakeSaveDiscountPort implements SaveDiscountPort {
        final List<Long> deletedIds = new ArrayList<>();

        @Override public Discount save(Discount d) { return d; }

        @Override
        public void delete(Long id) { deletedIds.add(id); }
    }
}
