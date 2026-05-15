package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
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

class RefundOrderServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static final User ADMIN_USER = new User(
            99L, new PhoneNumber("992000000099"), UserRole.ADMIN,
            "Admin", null, LoyaltyProfile.empty(), true, 1L);

    static final User MANAGER_USER = new User(
            88L, new PhoneNumber("992000000088"), UserRole.MANAGER,
            "Manager", null, LoyaltyProfile.empty(), true, 1L);

    static Order orderWithStatus(OrderStatus status) {
        return new Order.Builder()
                .id(1L).userId(10L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
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
    }

    static RefundOrderService service(Order order, User requester,
                                      CapturingSaveOrderPort save,
                                      NotificationPort notifPort) {
        return new RefundOrderService(
                orderPort(order),
                save,
                userPort(requester),
                new OrderNotificationService(notifPort));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Refunds a DELIVERED order — status becomes REFUNDED")
    void refundsDeliveredOrder() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        service(orderWithStatus(OrderStatus.DELIVERED), ADMIN_USER, save,
                new CountingNotificationPort())
                .execute(1L, ADMIN_USER.id(), "Customer dispute");

        assertEquals(OrderStatus.REFUNDED, save.last().status());
    }

    @Test
    @DisplayName("Refunds a CANCELLED order — status becomes REFUNDED")
    void refundsCancelledOrder() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        service(orderWithStatus(OrderStatus.CANCELLED), ADMIN_USER, save,
                new CountingNotificationPort())
                .execute(1L, ADMIN_USER.id(), null);

        assertEquals(OrderStatus.REFUNDED, save.last().status());
    }

    @Test
    @DisplayName("Refund sets refundedAt timestamp")
    void refund_setsRefundedAt() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();

        Instant before = Instant.now();
        service(orderWithStatus(OrderStatus.DELIVERED), ADMIN_USER, save,
                new CountingNotificationPort())
                .execute(1L, ADMIN_USER.id(), null);
        Instant after = Instant.now();

        Order saved = save.last();
        assertNotNull(saved.refundedAt());
        assertFalse(saved.refundedAt().isBefore(before));
        assertFalse(saved.refundedAt().isAfter(after));
    }

    @Test
    @DisplayName("Rejects a PAID order — not a final state")
    void rejectsNonFinalStatus_throws() {
        RefundOrderService svc = service(orderWithStatus(OrderStatus.PAID), ADMIN_USER,
                new CapturingSaveOrderPort(), new CountingNotificationPort());

        assertThrows(IllegalStateException.class, () -> svc.execute(1L, ADMIN_USER.id(), null));
    }

    @Test
    @DisplayName("Rejects MANAGER requester — ADMIN only")
    void rejectsNonAdminRequester_throws() {
        RefundOrderService svc = service(orderWithStatus(OrderStatus.DELIVERED), MANAGER_USER,
                new CapturingSaveOrderPort(), new CountingNotificationPort());

        assertThrows(IllegalStateException.class, () -> svc.execute(1L, MANAGER_USER.id(), null));
    }

    @Test
    @DisplayName("Notification fires with REFUNDED status after successful refund")
    void notifiesOnRefund() {
        CountingNotificationPort port = new CountingNotificationPort();
        CapturingSaveOrderPort   save = new CapturingSaveOrderPort();

        service(orderWithStatus(OrderStatus.DELIVERED), ADMIN_USER, save, port)
                .execute(1L, ADMIN_USER.id(), null);

        assertEquals(1, port.updateCount);
        assertEquals(OrderStatus.REFUNDED, port.lastStatus);
    }
}
