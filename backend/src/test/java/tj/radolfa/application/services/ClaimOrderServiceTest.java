package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.ClaimOrderUseCase;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.ClaimOrderPort;
import tj.radolfa.domain.exception.OrderAlreadyClaimedException;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClaimOrderServiceTest {

    static final long ORDER_ID   = 1L;
    static final long COURIER_ID = 99L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    /** Simulates the conditional-UPDATE adapter: succeeds once per order, false thereafter. */
    static class FakeClaimOrderPort implements ClaimOrderPort {
        private boolean available;
        FakeClaimOrderPort(boolean available) { this.available = available; }

        @Override
        public boolean claimIfAvailable(Long orderId, Long courierId, Instant claimedAt) {
            if (available) {
                available = false;
                return true;
            }
            return false;
        }
    }

    /** Records how many times generate was called; returns a stub DeliveryCode. */
    static class RecordingDeliveryCodeUseCase implements GenerateDeliveryCodeUseCase {
        final List<Long> calls = new ArrayList<>();

        @Override
        public DeliveryCode execute(Long orderId) {
            calls.add(orderId);
            return new DeliveryCode(1L, orderId, "00001234",
                    Instant.now().plusSeconds(72 * 3600), null, 0, Instant.now());
        }
    }

    static ClaimOrderService service(ClaimOrderPort claimPort,
                                     GenerateDeliveryCodeUseCase codeUseCase) {
        return new ClaimOrderService(claimPort, codeUseCase);
    }

    static Order pickedHomeOrder() {
        return new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.PICKED)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Toshkent, Chilonzor")
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Available HOME PICKED order → claim succeeds and delivery code is generated")
    void availableOrder_claimSucceeds_codeGenerated() {
        var codeUseCase = new RecordingDeliveryCodeUseCase();
        var svc = service(new FakeClaimOrderPort(true), codeUseCase);

        assertDoesNotThrow(() -> svc.execute(new ClaimOrderUseCase.Command(ORDER_ID, COURIER_ID)));
        assertEquals(1, codeUseCase.calls.size(), "Delivery code must be generated exactly once");
        assertEquals(ORDER_ID, codeUseCase.calls.get(0));
    }

    @Test
    @DisplayName("Second claim on same order → OrderAlreadyClaimedException, no delivery code generated")
    void secondClaim_throwsAlreadyClaimed_noCodeGenerated() {
        var codeUseCase = new RecordingDeliveryCodeUseCase();
        var claimPort   = new FakeClaimOrderPort(true);
        var svc         = service(claimPort, codeUseCase);

        // First claim succeeds
        svc.execute(new ClaimOrderUseCase.Command(ORDER_ID, COURIER_ID));

        // Second claim from a different courier should fail
        assertThrows(OrderAlreadyClaimedException.class,
                () -> svc.execute(new ClaimOrderUseCase.Command(ORDER_ID, 200L)));

        assertEquals(1, codeUseCase.calls.size(), "Delivery code must not be generated on failed claim");
    }

    @Test
    @DisplayName("Port returns false (already claimed/wrong state) → OrderAlreadyClaimedException, no code")
    void portReturnsFalse_throwsImmediately_noCodeGenerated() {
        var codeUseCase = new RecordingDeliveryCodeUseCase();
        var svc = service(new FakeClaimOrderPort(false), codeUseCase);

        assertThrows(OrderAlreadyClaimedException.class,
                () -> svc.execute(new ClaimOrderUseCase.Command(ORDER_ID, COURIER_ID)));
        assertTrue(codeUseCase.calls.isEmpty(), "Delivery code must not be generated when claim fails");
    }
}
