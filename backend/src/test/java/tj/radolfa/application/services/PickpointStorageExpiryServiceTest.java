package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PickpointStorageExpiryServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

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
        final List<Order> expiring;
        final List<Order> warning;

        FakeLoadExpiringPickpointOrdersPort(List<Order> expiring, List<Order> warning) {
            this.expiring = expiring;
            this.warning  = warning;
        }

        @Override public List<Order> findReadyForPickupOlderThan(Instant cutoff) { return expiring; }
        @Override public List<Order> findReadyForPickupInWindow(Instant s, Instant e) { return warning; }
    }

    static class CountingExpireOrderUseCase implements ExpireOrderUseCase {
        final List<Long> expiredOrderIds = new ArrayList<>();
        @Override public void execute(Long orderId, String reason) { expiredOrderIds.add(orderId); }
    }

    static class ThrowingExpireOrderUseCase implements ExpireOrderUseCase {
        final AtomicInteger calls = new AtomicInteger(0);
        @Override public void execute(Long orderId, String reason) {
            calls.incrementAndGet();
            throw new IllegalStateException("simulated failure for orderId=" + orderId);
        }
    }

    static class CountingNotificationPort implements NotificationPort {
        final List<Long> warnedOrders   = new ArrayList<>();
        final List<Long> cancelledOrders = new ArrayList<>();
        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) { warnedOrders.add(o); }
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) { cancelledOrders.add(o); }
    }

    static PickpointStorageExpiryService service(List<Order> expiring,
                                                  List<Order> warning,
                                                  ExpireOrderUseCase expireUseCase,
                                                  NotificationPort notifPort) {
        return new PickpointStorageExpiryService(
                new FakeLoadExpiringPickpointOrdersPort(expiring, warning),
                expireUseCase, notifPort, STORAGE_DAYS, WARNING_DAYS);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Expiry batch: 3 expired orders → expire use case called 3 times + 3 cancellation notifications")
    void expiryBatch_cancelsAllExpiredOrders() {
        List<Order> expiring = List.of(pickpointOrder(1), pickpointOrder(2), pickpointOrder(3));
        CountingExpireOrderUseCase expire = new CountingExpireOrderUseCase();
        CountingNotificationPort   notif  = new CountingNotificationPort();

        service(expiring, List.of(), expire, notif).runDailySweep();

        assertEquals(3, expire.expiredOrderIds.size());
        assertEquals(3, notif.cancelledOrders.size());
        assertTrue(notif.warnedOrders.isEmpty());
    }

    @Test
    @DisplayName("Warning batch: 2 warning-window orders → 2 warning notifications, no cancellations")
    void warningBatch_sendsWarningNotifications() {
        List<Order> warning = List.of(pickpointOrder(4), pickpointOrder(5));
        CountingExpireOrderUseCase expire = new CountingExpireOrderUseCase();
        CountingNotificationPort   notif  = new CountingNotificationPort();

        service(List.of(), warning, expire, notif).runDailySweep();

        assertTrue(expire.expiredOrderIds.isEmpty());
        assertTrue(notif.cancelledOrders.isEmpty());
        assertEquals(2, notif.warnedOrders.size());
    }

    @Test
    @DisplayName("Recent orders are not expired or warned")
    void recentOrders_noAction() {
        CountingExpireOrderUseCase expire = new CountingExpireOrderUseCase();
        CountingNotificationPort   notif  = new CountingNotificationPort();

        service(List.of(), List.of(), expire, notif).runDailySweep();

        assertTrue(expire.expiredOrderIds.isEmpty());
        assertTrue(notif.cancelledOrders.isEmpty());
        assertTrue(notif.warnedOrders.isEmpty());
    }

    @Test
    @DisplayName("One expiry failure does not abort remaining orders in the batch")
    void expiryFailure_doesNotAbortBatch() {
        List<Order> expiring = List.of(pickpointOrder(1), pickpointOrder(2), pickpointOrder(3));
        ThrowingExpireOrderUseCase throwing = new ThrowingExpireOrderUseCase();
        CountingNotificationPort   notif    = new CountingNotificationPort();

        // Should not throw — failures are swallowed per-iteration
        assertDoesNotThrow(() -> service(expiring, List.of(), throwing, notif).runDailySweep());
        assertEquals(3, throwing.calls.get()); // all 3 were attempted
        assertTrue(notif.cancelledOrders.isEmpty()); // notifications never fired because exception thrown before them
    }
}
