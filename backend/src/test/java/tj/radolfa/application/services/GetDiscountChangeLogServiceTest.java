package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.LoadDiscountChangePort;
import tj.radolfa.domain.model.ChangeType;
import tj.radolfa.domain.model.DiscountChange;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetDiscountChangeLogServiceTest {

    private static final Long DISCOUNT_ID = 42L;

    private FakeLoadDiscountChangePort fakeLoad;
    private GetDiscountChangeLogService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadDiscountChangePort();
        service = new GetDiscountChangeLogService(fakeLoad);
    }

    @Test
    @DisplayName("returns the page produced by the port for the given discount and pageable")
    void returnsPageFromPort() {
        DiscountChange create = new DiscountChange(1L, DISCOUNT_ID, ChangeType.CREATE,
                null, "{\"amountValue\":10}", 7L, Instant.parse("2026-01-01T00:00:00Z"));
        DiscountChange update = new DiscountChange(2L, DISCOUNT_ID, ChangeType.UPDATE,
                "{\"amountValue\":10}", "{\"amountValue\":15}", 7L, Instant.parse("2026-01-02T00:00:00Z"));
        fakeLoad.store(DISCOUNT_ID, List.of(update, create));

        Pageable pageable = PageRequest.of(0, 20);
        Page<DiscountChange> result = service.execute(DISCOUNT_ID, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of(update, create), result.getContent());
        assertEquals(DISCOUNT_ID, fakeLoad.lastDiscountId);
        assertEquals(pageable, fakeLoad.lastPageable);
    }

    static class FakeLoadDiscountChangePort implements LoadDiscountChangePort {
        private List<DiscountChange> changes = List.of();
        Long lastDiscountId;
        Pageable lastPageable;

        void store(Long discountId, List<DiscountChange> rows) {
            this.changes = rows;
        }

        @Override
        public Page<DiscountChange> findByDiscountId(Long discountId, Pageable pageable) {
            this.lastDiscountId = discountId;
            this.lastPageable = pageable;
            return new PageImpl<>(changes, pageable, changes.size());
        }
    }
}
