package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InitiateReturnToWarehouseServiceTest {

    static final Long PICKPOINT_ID = 5L;
    static final Long ORDER_ID     = 1L;
    static final Long USER_ID      = 10L;

    // ── Factories ────────────────────────────────────────────────────────────

    static Order readyOrder() {
        return new Order.Builder()
                .id(ORDER_ID).userId(USER_ID)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryType(DeliveryType.PICKPOINT)
                .pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(200)))
                .createdAt(Instant.now())
                .build();
    }

    static User user(UserRole role, Long pickpointId) {
        return new User(99L, new PhoneNumber("+992000000099"), role,
                "Test User", null, null, true, 1L,
                null, null, null, null, null, pickpointId, null);
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

    static LoadUserPort userPort(User u) {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) {
                return u != null && u.id().equals(id) ? Optional.of(u) : Optional.empty();
            }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
        };
    }

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order order) { saved.add(order); return order; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class CapturingNotificationPort implements NotificationPort {
        final List<Long[]> returnInitiatedCalls = new ArrayList<>();
        @Override public void sendReturnInitiatedNotification(Long uid, Long oid) {
            returnInitiatedCalls.add(new Long[]{uid, oid});
        }
        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
    }

    static InitiateReturnToWarehouseService service(Order order, User actor,
                                                     CapturingSaveOrderPort saveOrder,
                                                     CapturingNotificationPort notif) {
        return new InitiateReturnToWarehouseService(
                orderPort(order), saveOrder, userPort(actor), notif);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PICKPOINT_STAFF at correct pickpoint → RETURN_INITIATED, audit fields set, notification sent")
    void staffAtCorrectPickpoint_transitionsAndNotifies() {
        var saveOrder = new CapturingSaveOrderPort();
        var notif     = new CapturingNotificationPort();
        service(readyOrder(), user(UserRole.PICKPOINT_STAFF, PICKPOINT_ID), saveOrder, notif)
                .execute(ORDER_ID, 99L);

        assertEquals(OrderStatus.RETURN_INITIATED, saveOrder.last().status());
        assertNotNull(saveOrder.last().returnInitiatedAt());
        assertEquals(99L, saveOrder.last().returnInitiatedByUserId());
        assertEquals(1, notif.returnInitiatedCalls.size());
        assertEquals(USER_ID, notif.returnInitiatedCalls.get(0)[0]);
    }

    @Test
    @DisplayName("PICKPOINT_STAFF at different pickpoint → PickpointAccessDeniedException")
    void staffWrongPickpoint_throwsAccessDenied() {
        assertThrows(PickpointAccessDeniedException.class,
                () -> service(readyOrder(), user(UserRole.PICKPOINT_STAFF, 99L),
                        new CapturingSaveOrderPort(), new CapturingNotificationPort())
                        .execute(ORDER_ID, 99L));
    }

    @Test
    @DisplayName("PICKPOINT_STAFF with null pickpointId → PickpointAccessDeniedException")
    void staffNullPickpoint_throwsAccessDenied() {
        assertThrows(PickpointAccessDeniedException.class,
                () -> service(readyOrder(), user(UserRole.PICKPOINT_STAFF, null),
                        new CapturingSaveOrderPort(), new CapturingNotificationPort())
                        .execute(ORDER_ID, 99L));
    }

    @Test
    @DisplayName("ADMIN initiating for any order → succeeds (no pickpoint check)")
    void adminAnyPickpoint_succeeds() {
        var saveOrder = new CapturingSaveOrderPort();
        service(readyOrder(), user(UserRole.ADMIN, null), saveOrder, new CapturingNotificationPort())
                .execute(ORDER_ID, 99L);
        assertEquals(OrderStatus.RETURN_INITIATED, saveOrder.last().status());
    }

    @Test
    @DisplayName("MANAGER initiating → succeeds (admin-like, no pickpoint check)")
    void managerInitiating_succeeds() {
        var saveOrder = new CapturingSaveOrderPort();
        service(readyOrder(), user(UserRole.MANAGER, null), saveOrder, new CapturingNotificationPort())
                .execute(ORDER_ID, 99L);
        assertEquals(OrderStatus.RETURN_INITIATED, saveOrder.last().status());
    }

    @Test
    @DisplayName("Order not READY_FOR_PICKUP → IllegalStateException")
    void orderNotReady_throwsIllegalState() {
        Order paidOrder = new Order.Builder()
                .id(ORDER_ID).userId(USER_ID).status(OrderStatus.PAID)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .build();
        assertThrows(IllegalStateException.class,
                () -> service(paidOrder, user(UserRole.PICKPOINT_STAFF, PICKPOINT_ID),
                        new CapturingSaveOrderPort(), new CapturingNotificationPort())
                        .execute(ORDER_ID, 99L));
    }

    @Test
    @DisplayName("Order not found → ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(null, user(UserRole.PICKPOINT_STAFF, PICKPOINT_ID),
                        new CapturingSaveOrderPort(), new CapturingNotificationPort())
                        .execute(999L, 99L));
    }
}
