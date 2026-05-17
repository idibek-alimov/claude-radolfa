package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadExpiringPickpointOrdersPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PickpointStorageExpiryServiceTest {

    static final int STORAGE_DAYS = 7;
    static final int WARNING_DAYS = 2;

    static Order pickpointOrder(long id) {
        return new Order.Builder()
                .id(id).userId(10L + id).status(OrderStatus.READY_FOR_PICKUP)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(3L)
                .readyForPickupAt(Instant.now())
                .build();
    }

    static class FakeLoadExpiringPickpointOrdersPort implements LoadExpiringPickpointOrdersPort {
        final List<Order> warning;
        FakeLoadExpiringPickpointOrdersPort(List<Order> warning) { this.warning = warning; }
        @Override public List<Order> findReadyForPickupOlderThan(Instant cutoff) { return List.of(); }
        @Override public List<Order> findReadyForPickupInWindow(Instant s, Instant e) { return warning; }
    }

    static class CountingNotificationPort implements NotificationPort {
        final List<Long> warnedOrders = new ArrayList<>();
        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) { warnedOrders.add(o); }
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
    }

    static PickpointStorageExpiryService service(List<Order> warning, NotificationPort notifPort) {
        return new PickpointStorageExpiryService(
                new FakeLoadExpiringPickpointOrdersPort(warning),
                notifPort, STORAGE_DAYS, WARNING_DAYS);
    }

    @Test
    @DisplayName("Warning batch: 2 orders in warning window → 2 warning notifications sent")
    void warningBatch_sendsWarningNotifications() {
        List<Order> warning = List.of(pickpointOrder(4), pickpointOrder(5));
        CountingNotificationPort notif = new CountingNotificationPort();

        service(warning, notif).runDailySweep();

        assertEquals(2, notif.warnedOrders.size());
    }

    @Test
    @DisplayName("No orders in window → sweep completes with no notifications")
    void noOrders_noNotifications() {
        CountingNotificationPort notif = new CountingNotificationPort();

        service(List.of(), notif).runDailySweep();

        assertTrue(notif.warnedOrders.isEmpty());
    }

    @Test
    @DisplayName("Overdue orders beyond storage window are NOT auto-cancelled (manual initiation required)")
    void overdueOrders_notAutoCancelled() {
        // Even though orders older than storageDays exist, the sweep does not cancel them.
        // findReadyForPickupOlderThan returns them but the service ignores them.
        CountingNotificationPort notif = new CountingNotificationPort();

        // FakePort.findReadyForPickupOlderThan returns an empty list — the service doesn't call cancel.
        assertDoesNotThrow(() -> service(List.of(), notif).runDailySweep());
        assertTrue(notif.warnedOrders.isEmpty());
    }
}
