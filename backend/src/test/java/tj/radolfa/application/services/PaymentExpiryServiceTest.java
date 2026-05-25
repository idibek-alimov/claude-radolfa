package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PaymentExpiryServiceTest {

    static final int TIMEOUT_MINUTES = 30;

    static Order pendingOrder(long id) {
        return new Order.Builder()
                .id(id).userId(100L + id).status(OrderStatus.PENDING)
                .totalAmount(new Money(BigDecimal.valueOf(150)))
                .createdAt(Instant.now().minus(60, ChronoUnit.MINUTES))
                .deliveryType(DeliveryType.HOME)
                .build();
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    static class FakeLoadOrderPort implements LoadOrderPort {
        private final List<Order> expired;

        FakeLoadOrderPort(List<Order> expired) { this.expired = expired; }

        @Override
        public List<Order> findExpiredPending(Instant cutoff) { return expired; }

        @Override public List<Order> loadByUserId(Long userId)              { throw new UnsupportedOperationException(); }
        @Override public Optional<Order> loadById(Long id)                  { throw new UnsupportedOperationException(); }
        @Override public Optional<Order> loadByExternalOrderId(String eid)  { throw new UnsupportedOperationException(); }
        @Override public List<Order> loadRecentPaidByUserId(Long u, int l)  { throw new UnsupportedOperationException(); }
    }

    static class RecordingExpireOrderUseCase implements ExpireOrderUseCase {
        final List<Long> expiredIds = new ArrayList<>();
        Long throwOnId = null;

        @Override
        public void execute(Long orderId, String reason) {
            if (orderId.equals(throwOnId)) throw new RuntimeException("forced expiry failure");
            expiredIds.add(orderId);
        }
    }

    static PaymentExpiryService service(List<Order> stale, RecordingExpireOrderUseCase useCase) {
        return new PaymentExpiryService(new FakeLoadOrderPort(stale), useCase, TIMEOUT_MINUTES);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Two stale PENDING orders → both expired")
    void twoStaleOrders_bothExpired() {
        RecordingExpireOrderUseCase useCase = new RecordingExpireOrderUseCase();

        service(List.of(pendingOrder(1), pendingOrder(2)), useCase).runSweep();

        assertEquals(List.of(1L, 2L), useCase.expiredIds);
    }

    @Test
    @DisplayName("No stale orders returned → expiry use case never called")
    void noStaleOrders_noExpiry() {
        RecordingExpireOrderUseCase useCase = new RecordingExpireOrderUseCase();

        service(List.of(), useCase).runSweep();

        assertTrue(useCase.expiredIds.isEmpty());
    }

    @Test
    @DisplayName("One order throws on expiry → remaining orders still processed")
    void oneOrderFails_restStillExpired() {
        RecordingExpireOrderUseCase useCase = new RecordingExpireOrderUseCase();
        useCase.throwOnId = 1L;

        assertDoesNotThrow(() ->
                service(List.of(pendingOrder(1), pendingOrder(2)), useCase).runSweep());

        assertEquals(List.of(2L), useCase.expiredIds);
    }

    @Test
    @DisplayName("Empty stale list → sweep exits cleanly with no errors")
    void emptyList_sweepCompletesCleanly() {
        RecordingExpireOrderUseCase useCase = new RecordingExpireOrderUseCase();

        assertDoesNotThrow(() -> service(List.of(), useCase).runSweep());
        assertTrue(useCase.expiredIds.isEmpty());
    }
}
