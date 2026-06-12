package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RetryDeliveryServiceTest {

    static final long COURIER_ID = 99L;

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static Order attemptedOrder() {
        return new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.DELIVERY_ATTEMPTED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(COURIER_ID).deliveryAttemptCount(1)
                .build();
    }

    static LoadOrderPort orderPort(Order order) {
        return new LoadOrderPort() {
            @Override public Optional<Order> loadById(Long id) {
                return order != null && order.id().equals(id) ? Optional.of(order) : Optional.empty();
            }
            @Override public List<Order> loadByUserId(Long u)                       { return List.of(); }
            @Override public Optional<Order> loadByExternalOrderId(String e)        { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long u, int l)      { return List.of(); }
        };
    }

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order o) { saved.add(o); return o; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class RecordingDeliveryCodeUseCase implements GenerateDeliveryCodeUseCase {
        final List<Long> calls = new ArrayList<>();
        @Override public DeliveryCode execute(Long orderId) {
            calls.add(orderId);
            return new DeliveryCode(1L, orderId, "00001234",
                    Instant.now().plusSeconds(72 * 3600), null, 0, Instant.now());
        }
    }

    static NotificationPort silentPort() {
        return new NotificationPort() {
            @Override public void sendOrderConfirmation(Long u, Long o) {}
            @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
            @Override public void sendReviewApprovedNotification(Long u, Long r) {}
            @Override public void sendReviewReplyNotification(Long u, Long r) {}
            @Override public void sendDeliveryCode(Long u, Long o, String c, Instant e) {}
            @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
            @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
        };
    }

    static RetryDeliveryService service(Order order,
                                        CapturingSaveOrderPort save,
                                        RecordingDeliveryCodeUseCase codeUseCase) {
        return new RetryDeliveryService(orderPort(order), save,
                new OrderNotificationService(silentPort()), codeUseCase);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELIVERY_ATTEMPTED with matching courier → OUT_FOR_DELIVERY, outForDeliveryAt set, fresh code generated")
    void attemptedWithMatchingCourier_becomesOutForDelivery_codeGenerated() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        RecordingDeliveryCodeUseCase codeUseCase = new RecordingDeliveryCodeUseCase();
        RetryDeliveryService svc = service(attemptedOrder(), save, codeUseCase);

        Instant before = Instant.now();
        svc.execute(1L, COURIER_ID);
        Instant after = Instant.now();

        Order saved = save.last();
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, saved.status());
        assertNotNull(saved.outForDeliveryAt());
        assertFalse(saved.outForDeliveryAt().isBefore(before));
        assertFalse(saved.outForDeliveryAt().isAfter(after));

        assertEquals(1, codeUseCase.calls.size(), "Fresh delivery code must be generated exactly once");
        assertEquals(1L, codeUseCase.calls.get(0));
    }

    @Test
    @DisplayName("Wrong status (PICKED) → throws IllegalStateException, no code generated")
    void wrongStatus_throwsIllegalState_noCodeGenerated() {
        Order pickedOrder = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.PICKED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(COURIER_ID)
                .build();
        RecordingDeliveryCodeUseCase codeUseCase = new RecordingDeliveryCodeUseCase();
        RetryDeliveryService svc = service(pickedOrder, new CapturingSaveOrderPort(), codeUseCase);

        assertThrows(IllegalStateException.class, () -> svc.execute(1L, COURIER_ID));
        assertTrue(codeUseCase.calls.isEmpty(), "Delivery code must not be generated on status mismatch");
    }

    @Test
    @DisplayName("Mismatched courierId → throws CourierAccessDeniedException")
    void mismatchedCourier_throwsAccessDenied() {
        RetryDeliveryService svc = service(attemptedOrder(), new CapturingSaveOrderPort(),
                new RecordingDeliveryCodeUseCase());

        assertThrows(CourierAccessDeniedException.class, () -> svc.execute(1L, 42L));
    }

    @Test
    @DisplayName("Null courierId on order → throws CourierAccessDeniedException")
    void nullCourierOnOrder_throwsAccessDenied() {
        Order noCourierOrder = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.DELIVERY_ATTEMPTED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .build();
        RetryDeliveryService svc = service(noCourierOrder, new CapturingSaveOrderPort(),
                new RecordingDeliveryCodeUseCase());

        assertThrows(CourierAccessDeniedException.class, () -> svc.execute(1L, COURIER_ID));
    }

    @Test
    @DisplayName("Order not found → throws ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFoundException() {
        RetryDeliveryService svc = service(null, new CapturingSaveOrderPort(),
                new RecordingDeliveryCodeUseCase());

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L, COURIER_ID));
    }
}
