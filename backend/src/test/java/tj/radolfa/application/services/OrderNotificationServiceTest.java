package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.NotificationPort;
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

class OrderNotificationServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class RecordingNotificationPort implements NotificationPort {
        record Call(String method, Long userId, Long orderId, OrderStatus status) {}
        final List<Call> calls = new ArrayList<>();

        @Override
        public void sendOrderConfirmation(Long userId, Long orderId) {
            calls.add(new Call("confirm", userId, orderId, null));
        }

        @Override
        public void sendOrderStatusUpdate(Long userId, Long orderId, OrderStatus status) {
            calls.add(new Call("update", userId, orderId, status));
        }

        @Override public void sendReviewApprovedNotification(Long userId, Long reviewId) {}
        @Override public void sendReviewReplyNotification(Long userId, Long reviewId) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
    }

    static class ThrowingNotificationPort implements NotificationPort {
        @Override public void sendOrderConfirmation(Long u, Long o) { throw new RuntimeException("provider down"); }
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) { throw new RuntimeException("provider down"); }
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
    }

    static Order orderWithStatus(OrderStatus status) {
        return new Order.Builder()
                .id(42L).userId(7L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(100))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME)
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PAID triggers sendOrderConfirmation with correct userId and orderId")
    void paid_sendsConfirmation() {
        RecordingNotificationPort port = new RecordingNotificationPort();
        new OrderNotificationService(port).notify(orderWithStatus(OrderStatus.PAID));

        assertEquals(1, port.calls.size());
        assertEquals("confirm", port.calls.get(0).method());
        assertEquals(7L,  port.calls.get(0).userId());
        assertEquals(42L, port.calls.get(0).orderId());
    }

    @Test
    @DisplayName("SHIPPED triggers sendOrderStatusUpdate(SHIPPED)")
    void shipped_sendsStatusUpdate() {
        RecordingNotificationPort port = new RecordingNotificationPort();
        new OrderNotificationService(port).notify(orderWithStatus(OrderStatus.SHIPPED));

        assertEquals(1, port.calls.size());
        assertEquals("update", port.calls.get(0).method());
        assertEquals(OrderStatus.SHIPPED, port.calls.get(0).status());
    }

    @Test
    @DisplayName("READY_FOR_PICKUP triggers sendOrderStatusUpdate(READY_FOR_PICKUP)")
    void readyForPickup_sendsStatusUpdate() {
        RecordingNotificationPort port = new RecordingNotificationPort();
        new OrderNotificationService(port).notify(orderWithStatus(OrderStatus.READY_FOR_PICKUP));

        assertEquals(1, port.calls.size());
        assertEquals(OrderStatus.READY_FOR_PICKUP, port.calls.get(0).status());
    }

    @Test
    @DisplayName("DELIVERED triggers sendOrderStatusUpdate(DELIVERED)")
    void delivered_sendsStatusUpdate() {
        RecordingNotificationPort port = new RecordingNotificationPort();
        new OrderNotificationService(port).notify(orderWithStatus(OrderStatus.DELIVERED));

        assertEquals(1, port.calls.size());
        assertEquals(OrderStatus.DELIVERED, port.calls.get(0).status());
    }

    @Test
    @DisplayName("CANCELLED triggers sendOrderStatusUpdate(CANCELLED)")
    void cancelled_sendsStatusUpdate() {
        RecordingNotificationPort port = new RecordingNotificationPort();
        new OrderNotificationService(port).notify(orderWithStatus(OrderStatus.CANCELLED));

        assertEquals(1, port.calls.size());
        assertEquals(OrderStatus.CANCELLED, port.calls.get(0).status());
    }

    @Test
    @DisplayName("PENDING produces no notification")
    void pending_noNotification() {
        RecordingNotificationPort port = new RecordingNotificationPort();
        new OrderNotificationService(port).notify(orderWithStatus(OrderStatus.PENDING));

        assertTrue(port.calls.isEmpty());
    }

    @Test
    @DisplayName("Provider failure is swallowed — notify() returns normally")
    void providerFailure_doesNotPropagate() {
        OrderNotificationService svc = new OrderNotificationService(new ThrowingNotificationPort());
        assertDoesNotThrow(() -> svc.notify(orderWithStatus(OrderStatus.PAID)));
        assertDoesNotThrow(() -> svc.notify(orderWithStatus(OrderStatus.SHIPPED)));
    }
}
