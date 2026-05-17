package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.*;
import tj.radolfa.domain.exception.*;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VerifyPickupByCodeServiceTest {

    static final String VALID_CODE   = "654321";
    static final Long   ORDER_ID     = 1L;
    static final Long   PICKPOINT_ID = 5L;
    static final Long   STAFF_ID     = 99L;

    // ── Factories ────────────────────────────────────────────────────────────

    static DeliveryCode activeCode() {
        return new DeliveryCode(1L, ORDER_ID, VALID_CODE,
                Instant.now().plusSeconds(3600), null, 0, Instant.now());
    }

    static DeliveryCode expiredCode() {
        return new DeliveryCode(1L, ORDER_ID, VALID_CODE,
                Instant.now().minusSeconds(1), null, 0, Instant.now());
    }

    static DeliveryCode usedCode() {
        DeliveryCode c = activeCode(); c.markUsed(); return c;
    }

    static DeliveryCode exhaustedCode(int max) {
        return new DeliveryCode(1L, ORDER_ID, VALID_CODE,
                Instant.now().plusSeconds(3600), null, max, Instant.now());
    }

    static Order readyOrder(Long pickpointId) {
        return new Order.Builder()
                .id(ORDER_ID).userId(10L)
                .status(OrderStatus.READY_FOR_PICKUP)
                .deliveryType(DeliveryType.PICKPOINT)
                .pickpointId(pickpointId)
                .totalAmount(new Money(BigDecimal.valueOf(200)))
                .createdAt(Instant.now())
                .build();
    }

    static User staffUser(Long pickpointId) {
        return new User(STAFF_ID, new PhoneNumber("+992000000002"), UserRole.PICKPOINT_STAFF,
                "Staff", null, null, true, 1L,
                null, null, null, null, null, pickpointId, null);
    }

    static LoadDeliveryCodeByValuePort codePort(DeliveryCode code) {
        return c -> (code != null && code.getCode().equals(c)) ? Optional.of(code) : Optional.empty();
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
            @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
        };
    }

    static class CapturingSaveDeliveryCodePort implements SaveDeliveryCodePort {
        final List<DeliveryCode> saved = new ArrayList<>();
        @Override public DeliveryCode save(DeliveryCode c) { saved.add(c); return c; }
        @Override public void invalidateAllForOrder(Long id) {}
        DeliveryCode last() { return saved.get(saved.size() - 1); }
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
            @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
            @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
        };
    }

    static VerifyPickupByCodeService service(DeliveryCode code, Order order, User staff,
                                              CapturingSaveDeliveryCodePort saveCode,
                                              CapturingSaveOrderPort saveOrder) {
        return new VerifyPickupByCodeService(
                codePort(code), saveCode, orderPort(order), saveOrder,
                userPort(staff), new OrderNotificationService(silentPort()), 5);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid code at correct pickpoint → DELIVERED, code marked used")
    void validCode_orderDelivered() {
        var saveCode  = new CapturingSaveDeliveryCodePort();
        var saveOrder = new CapturingSaveOrderPort();
        service(activeCode(), readyOrder(PICKPOINT_ID), staffUser(PICKPOINT_ID),
                saveCode, saveOrder).execute(VALID_CODE, STAFF_ID);

        assertEquals(OrderStatus.DELIVERED, saveOrder.last().status());
        assertNotNull(saveOrder.last().deliveredAt());
        assertTrue(saveCode.last().isUsed());
    }

    @Test
    @DisplayName("Code belongs to order at different pickpoint → PickpointAccessDeniedException")
    void wrongPickpoint_throwsAccessDenied() {
        assertThrows(PickpointAccessDeniedException.class,
                () -> service(activeCode(), readyOrder(99L), staffUser(PICKPOINT_ID),
                        new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort())
                        .execute(VALID_CODE, STAFF_ID));
    }

    @Test
    @DisplayName("Code not found → DeliveryCodeNotFoundException")
    void codeNotFound_throwsNotFound() {
        assertThrows(DeliveryCodeNotFoundException.class,
                () -> service(null, readyOrder(PICKPOINT_ID), staffUser(PICKPOINT_ID),
                        new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort())
                        .execute(VALID_CODE, STAFF_ID));
    }

    @Test
    @DisplayName("Expired code → DeliveryCodeExpiredException")
    void expiredCode_throwsExpired() {
        assertThrows(DeliveryCodeExpiredException.class,
                () -> service(expiredCode(), readyOrder(PICKPOINT_ID), staffUser(PICKPOINT_ID),
                        new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort())
                        .execute(VALID_CODE, STAFF_ID));
    }

    @Test
    @DisplayName("Already used code → DeliveryCodeAlreadyUsedException")
    void alreadyUsed_throwsAlreadyUsed() {
        assertThrows(DeliveryCodeAlreadyUsedException.class,
                () -> service(usedCode(), readyOrder(PICKPOINT_ID), staffUser(PICKPOINT_ID),
                        new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort())
                        .execute(VALID_CODE, STAFF_ID));
    }

    @Test
    @DisplayName("Unknown code value → DeliveryCodeNotFoundException (lookup is by code value; wrong code means no match)")
    void unknownCode_throwsNotFound() {
        // VerifyPickupByCode looks up by code string — an unrecognised value yields no match
        assertThrows(DeliveryCodeNotFoundException.class,
                () -> service(activeCode(), readyOrder(PICKPOINT_ID), staffUser(PICKPOINT_ID),
                        new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort())
                        .execute("000000", STAFF_ID));
    }

    @Test
    @DisplayName("Attempts exhausted → DeliveryCodeAttemptsExhaustedException")
    void attemptsExhausted_throwsExhausted() {
        assertThrows(DeliveryCodeAttemptsExhaustedException.class,
                () -> service(exhaustedCode(5), readyOrder(PICKPOINT_ID), staffUser(PICKPOINT_ID),
                        new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort())
                        .execute(VALID_CODE, STAFF_ID));
    }

    @Test
    @DisplayName("Order not READY_FOR_PICKUP → IllegalStateException")
    void orderNotReady_throwsIllegalState() {
        Order notReady = new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.SHIPPED)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .build();

        assertThrows(IllegalStateException.class,
                () -> service(activeCode(), notReady, staffUser(PICKPOINT_ID),
                        new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort())
                        .execute(VALID_CODE, STAFF_ID));
    }
}
