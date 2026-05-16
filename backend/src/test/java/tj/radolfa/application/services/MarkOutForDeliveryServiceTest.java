package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
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

class MarkOutForDeliveryServiceTest {

    static final long COURIER_ID = 99L;

    static Order shippedOrder() {
        return new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.SHIPPED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(COURIER_ID)
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

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order o) { saved.add(o); return o; }
        Order last() { return saved.get(saved.size() - 1); }
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

    static MarkOutForDeliveryService service(Order order, CapturingSaveOrderPort save) {
        return new MarkOutForDeliveryService(orderPort(order), save,
                new OrderNotificationService(silentPort()));
    }

    @Test
    @DisplayName("SHIPPED order with matching courierId → status OUT_FOR_DELIVERY, outForDeliveryAt set")
    void shippedWithMatchingCourier_becomesOutForDelivery() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        MarkOutForDeliveryService svc = service(shippedOrder(), save);

        Instant before = Instant.now();
        svc.execute(1L, COURIER_ID);
        Instant after = Instant.now();

        Order saved = save.last();
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, saved.status());
        assertNotNull(saved.outForDeliveryAt());
        assertFalse(saved.outForDeliveryAt().isBefore(before));
        assertFalse(saved.outForDeliveryAt().isAfter(after));
    }

    @Test
    @DisplayName("PAID order → throws IllegalStateException")
    void wrongStatus_throwsIllegalState() {
        Order paidOrder = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.PAID)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(COURIER_ID)
                .build();

        MarkOutForDeliveryService svc = service(paidOrder, new CapturingSaveOrderPort());

        assertThrows(IllegalStateException.class, () -> svc.execute(1L, COURIER_ID));
    }

    @Test
    @DisplayName("Mismatched courierId → throws CourierAccessDeniedException")
    void mismatchedCourier_throwsAccessDenied() {
        MarkOutForDeliveryService svc = service(shippedOrder(), new CapturingSaveOrderPort());

        assertThrows(CourierAccessDeniedException.class, () -> svc.execute(1L, 42L));
    }

    @Test
    @DisplayName("Null courierId on order → throws CourierAccessDeniedException")
    void nullCourierOnOrder_throwsAccessDenied() {
        Order noCourierOrder = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.SHIPPED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .build();

        MarkOutForDeliveryService svc = service(noCourierOrder, new CapturingSaveOrderPort());

        assertThrows(CourierAccessDeniedException.class, () -> svc.execute(1L, COURIER_ID));
    }

    @Test
    @DisplayName("Order not found → throws ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFoundException() {
        MarkOutForDeliveryService svc = service(null, new CapturingSaveOrderPort());

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L, COURIER_ID));
    }
}
