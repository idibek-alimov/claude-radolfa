package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.loyalty.RestoreLoyaltyPointsUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CancelOrderServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static final User ADMIN_USER = new User(
            99L, new PhoneNumber("992000000099"), UserRole.ADMIN,
            "Admin", null, LoyaltyProfile.empty(), true, 1L);

    static final User REGULAR_USER = new User(
            10L, new PhoneNumber("992000000010"), UserRole.USER,
            "Alice", null, LoyaltyProfile.empty(), true, 1L);

    static Order pendingOrder(Long ownerId) {
        return new Order.Builder()
                .id(1L).userId(ownerId).status(OrderStatus.PENDING)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
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

    static LoadUserPort userPort(User user) {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) {
                return user != null && user.id().equals(id) ? Optional.of(user) : Optional.empty();
            }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(tj.radolfa.domain.model.UserRole r) { return List.of(); }
        };
    }

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order order) { saved.add(order); return order; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class CountingNotificationPort implements NotificationPort {
        int updateCount = 0;
        OrderStatus lastStatus;
        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) { updateCount++; lastStatus = s; }
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
    }

    static final StockAdjustmentPort NO_STOCK = new StockAdjustmentPort() {
        @Override public void decrement(Long skuId, int qty) {}
        @Override public void increment(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}
    };

    static final RestoreLoyaltyPointsUseCase NO_LOYALTY        = (userId, pts) -> {};
    static final tj.radolfa.application.ports.out.DeliveryEventPublisher NO_DELIVERY_EVENTS =
            new tj.radolfa.application.ports.out.DeliveryEventPublisher() {
                @Override public void publishOrderCancelledToCourier(Long c, Long o) {}
                @Override public void publishOrderAssignedToCourier(Long c, Long o) {}
                @Override public void publishNewOrderAtPickpoint(Long p, Long o) {}
                @Override public void publishOrderCancelledAtPickpoint(Long p, Long o) {}
                @Override public void publishDeliveryRetryLimitReached(Long o, Long c) {}
            };

    static CancelOrderService service(Order order, User requester,
                                      CapturingSaveOrderPort save,
                                      NotificationPort notifPort) {
        return new CancelOrderService(
                orderPort(order),
                save,
                userPort(requester),
                NO_STOCK,
                NO_LOYALTY,
                new OrderNotificationService(notifPort),
                NO_DELIVERY_EVENTS,
                new tj.radolfa.application.ports.out.LoadCartPort() {
                    @Override public java.util.Optional<tj.radolfa.domain.model.Cart> findActiveByUserId(Long id) { return java.util.Optional.empty(); }
                    @Override public java.util.Optional<tj.radolfa.domain.model.Cart> findById(Long id) { return java.util.Optional.empty(); }
                },
                cart -> cart);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("USER cancels own PENDING order — CANCELLED notification fires")
    void userCancelsOwnPendingOrder_notificationFired() {
        CountingNotificationPort port = new CountingNotificationPort();
        CapturingSaveOrderPort   save = new CapturingSaveOrderPort();

        service(pendingOrder(REGULAR_USER.id()), REGULAR_USER, save, port)
                .execute(1L, REGULAR_USER.id(), null);

        assertEquals(OrderStatus.CANCELLED, save.last().status());
        assertEquals(1, port.updateCount);
        assertEquals(OrderStatus.CANCELLED, port.lastStatus);
    }

    @Test
    @DisplayName("ADMIN cancels any PENDING order — CANCELLED notification fires")
    void adminCancelsOrder_notificationFired() {
        CountingNotificationPort port = new CountingNotificationPort();
        CapturingSaveOrderPort   save = new CapturingSaveOrderPort();

        service(pendingOrder(REGULAR_USER.id()), ADMIN_USER, save, port)
                .execute(1L, ADMIN_USER.id(), "admin override");

        assertEquals(OrderStatus.CANCELLED, save.last().status());
        assertEquals(1, port.updateCount);
    }

    @Test
    @DisplayName("USER cannot cancel another user's order")
    void userCannotCancelOtherOrder_throws() {
        Order otherUsersOrder = pendingOrder(55L);
        CancelOrderService svc = service(otherUsersOrder, REGULAR_USER,
                new CapturingSaveOrderPort(), new CountingNotificationPort());

        assertThrows(IllegalStateException.class,
                () -> svc.execute(1L, REGULAR_USER.id(), null));
    }

    @Test
    @DisplayName("Cancellation sets cancelledAt")
    void cancellation_setsCancelledAt() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        Instant before = Instant.now();
        service(pendingOrder(REGULAR_USER.id()), REGULAR_USER, save,
                new CountingNotificationPort())
                .execute(1L, REGULAR_USER.id(), null);
        Instant after = Instant.now();

        Order saved = save.last();
        assertNotNull(saved.cancelledAt());
        assertFalse(saved.cancelledAt().isBefore(before));
        assertFalse(saved.cancelledAt().isAfter(after));
    }

    @Test
    @DisplayName("ADMIN cannot cancel an already-DELIVERED order")
    void adminCannotCancelDeliveredOrder_throws() {
        Order delivered = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.DELIVERED)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .build();

        CancelOrderService svc = service(delivered, ADMIN_USER,
                new CapturingSaveOrderPort(), new CountingNotificationPort());

        assertThrows(IllegalStateException.class,
                () -> svc.execute(1L, ADMIN_USER.id(), null));
    }

    @Test
    @DisplayName("System path (ExpireOrderUseCase) cancels READY_FOR_PICKUP — status CANCELLED, notification fired")
    void systemExpiry_cancelsReadyForPickupOrder() {
        Order readyForPickup = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.READY_FOR_PICKUP)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(5L)
                .build();

        CapturingSaveOrderPort   save  = new CapturingSaveOrderPort();
        CountingNotificationPort notif = new CountingNotificationPort();
        CancelOrderService svc = service(readyForPickup, ADMIN_USER, save, notif);

        svc.execute(1L, "Pickup period expired");

        assertEquals(OrderStatus.CANCELLED, save.last().status());
        assertNotNull(save.last().cancelledAt());
        assertEquals(1, notif.updateCount);
        assertEquals(OrderStatus.CANCELLED, notif.lastStatus);
    }
}
