package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.RedirectToPickpointUseCase.Command;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Pickpoint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RedirectToPickpointServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static final long ORDER_ID    = 1L;
    static final long PICKPOINT_ID = 7L;

    static Order deliveryAttemptedHomeOrder() {
        return new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.DELIVERY_ATTEMPTED)
                .totalAmount(new Money(BigDecimal.valueOf(400))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Street 1")
                .courierId(55L).shippedAt(Instant.now()).outForDeliveryAt(Instant.now())
                .deliveryAttemptedAt(Instant.now()).deliveryAttemptCount(2)
                .build();
    }

    static LoadOrderPort orderPort(Order order) {
        return new LoadOrderPort() {
            @Override public Optional<Order> loadById(Long id) {
                return order != null && order.id().equals(id) ? Optional.of(order) : Optional.empty();
            }
            @Override public List<Order> loadByUserId(Long u) { return List.of(); }
            @Override public Optional<Order> loadByExternalOrderId(String e) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long u, int l) { return List.of(); }
        };
    }

    static LoadPickpointPort pickpointPort(boolean exists) {
        return new LoadPickpointPort() {
            @Override public List<Pickpoint> findAll(String s) { return List.of(); }
            @Override public List<Pickpoint> findAllActive() { return List.of(); }
            @Override public Optional<Pickpoint> findById(Long id) {
                return exists ? Optional.of(new Pickpoint(id, "P1", "Addr", true,
                        0.0, 0.0, false, false, false, false, "Asia/Dushanbe", false))
                              : Optional.empty();
            }
        };
    }

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order o) { saved.add(o); return o; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class CountingCodeUseCase implements GenerateDeliveryCodeUseCase {
        final AtomicInteger calls = new AtomicInteger(0);
        @Override public DeliveryCode execute(Long orderId) {
            calls.incrementAndGet();
            return null;
        }
    }

    static NotificationPort silentPort() {
        return new NotificationPort() {
            @Override public void sendOrderConfirmation(Long u, Long o) {}
            @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
            @Override public void sendReviewApprovedNotification(Long u, Long r) {}
            @Override public void sendReviewReplyNotification(Long u, Long r) {}
            @Override public void sendDeliveryCode(Long u, Long o, String c, Instant e) {}
        };
    }

    static RedirectToPickpointService service(LoadOrderPort orderPort,
                                               CapturingSaveOrderPort save,
                                               LoadPickpointPort pickpointPort,
                                               GenerateDeliveryCodeUseCase codeUseCase) {
        return new RedirectToPickpointService(orderPort, save, pickpointPort,
                codeUseCase, new OrderNotificationService(silentPort()));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELIVERY_ATTEMPTED + HOME → READY_FOR_PICKUP + PICKPOINT; courier cleared; timestamps cleared")
    void redirect_happyPath() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        CountingCodeUseCase code = new CountingCodeUseCase();

        service(orderPort(deliveryAttemptedHomeOrder()), save, pickpointPort(true), code)
                .execute(new Command(ORDER_ID, PICKPOINT_ID));

        Order saved = save.last();
        assertEquals(OrderStatus.READY_FOR_PICKUP, saved.status());
        assertEquals(DeliveryType.PICKPOINT, saved.deliveryType());
        assertEquals(PICKPOINT_ID, saved.pickpointId());
        assertNull(saved.courierId());
        assertNull(saved.shippedAt());
        assertNull(saved.outForDeliveryAt());
        assertNull(saved.deliveryAttemptedAt());
        assertEquals(2, saved.deliveryAttemptCount()); // audit trail preserved
        assertEquals(1, code.calls.get());
    }

    @Test
    @DisplayName("Wrong status (SHIPPED) → IllegalStateException, nothing saved")
    void wrongStatus_throwsIllegalState() {
        Order shipped = new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.SHIPPED)
                .totalAmount(new Money(BigDecimal.valueOf(400))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr").build();
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        assertThrows(IllegalStateException.class, () ->
                service(orderPort(shipped), save, pickpointPort(true), new CountingCodeUseCase())
                        .execute(new Command(ORDER_ID, PICKPOINT_ID)));

        assertTrue(save.saved.isEmpty());
    }

    @Test
    @DisplayName("Already a PICKPOINT order → IllegalStateException")
    void alreadyPickpoint_throwsIllegalState() {
        Order pickpointAttempted = new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.DELIVERY_ATTEMPTED)
                .totalAmount(new Money(BigDecimal.valueOf(400))).createdAt(Instant.now())
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(3L).build();
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        assertThrows(IllegalStateException.class, () ->
                service(orderPort(pickpointAttempted), save, pickpointPort(true), new CountingCodeUseCase())
                        .execute(new Command(ORDER_ID, PICKPOINT_ID)));

        assertTrue(save.saved.isEmpty());
    }

    @Test
    @DisplayName("Pickpoint does not exist → ResourceNotFoundException")
    void missingPickpoint_throwsResourceNotFound() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        assertThrows(ResourceNotFoundException.class, () ->
                service(orderPort(deliveryAttemptedHomeOrder()), save, pickpointPort(false),
                        new CountingCodeUseCase())
                        .execute(new Command(ORDER_ID, PICKPOINT_ID)));

        assertTrue(save.saved.isEmpty());
    }
}
