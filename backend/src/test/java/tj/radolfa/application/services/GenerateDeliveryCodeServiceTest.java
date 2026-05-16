package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveDeliveryCodePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryCode;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GenerateDeliveryCodeServiceTest {

    // ── Fakes ────────────────────────────────────────────────────────────────

    static final User CUSTOMER = new User(
            10L, new PhoneNumber("992000000010"), UserRole.USER,
            "Alice", null, LoyaltyProfile.empty(), true, 1L);

    static Order order(OrderStatus status, DeliveryType type) {
        return new Order.Builder()
                .id(1L).userId(10L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(type).deliveryAddress("Addr")
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

    static class CapturingSaveDeliveryCodePort implements SaveDeliveryCodePort {
        final List<DeliveryCode> saved      = new ArrayList<>();
        final List<Long>         invalidated = new ArrayList<>();
        int saveCallCount = 0;

        @Override
        public DeliveryCode save(DeliveryCode code) {
            saved.add(code);
            saveCallCount++;
            return new DeliveryCode(1L, code.getOrderId(), code.getCode(),
                    code.getExpiresAt(), code.getUsedAt(), code.getAttemptCount(), code.getCreatedAt());
        }

        @Override
        public void invalidateAllForOrder(Long orderId) {
            invalidated.add(orderId);
        }

        DeliveryCode lastSaved() { return saved.get(saved.size() - 1); }
    }

    static class CapturingNotificationPort implements NotificationPort {
        record DeliveryCodeCall(Long userId, Long orderId, String code, Instant expiresAt) {}
        final List<DeliveryCodeCall> deliveryCodeCalls = new ArrayList<>();

        @Override public void sendOrderConfirmation(Long u, Long o) {}
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}

        @Override
        public void sendDeliveryCode(Long userId, Long orderId, String code, Instant expiresAt) {
            deliveryCodeCalls.add(new DeliveryCodeCall(userId, orderId, code, expiresAt));
        }
    }

    static GenerateDeliveryCodeService service(Order order,
                                               CapturingSaveDeliveryCodePort save,
                                               NotificationPort notif) {
        return new GenerateDeliveryCodeService(
                orderPort(order), userPort(CUSTOMER), save, notif, 72);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHIPPED order → 6-digit code generated, saved, and SMS notification sent")
    void shippedOrder_codeGeneratedAndSmsSent() {
        CapturingSaveDeliveryCodePort save  = new CapturingSaveDeliveryCodePort();
        CapturingNotificationPort     notif = new CapturingNotificationPort();
        GenerateDeliveryCodeService   svc   = service(
                order(OrderStatus.SHIPPED, DeliveryType.HOME), save, notif);

        DeliveryCode result = svc.execute(1L);

        assertNotNull(result);
        assertTrue(result.getCode().matches("[0-9]{6}"),
                "Code must be 6 digits, was: " + result.getCode());
        assertEquals(1, save.saveCallCount);
        assertEquals(1, save.invalidated.size());
        assertEquals(1, notif.deliveryCodeCalls.size());
        assertEquals(10L, notif.deliveryCodeCalls.get(0).userId());
        assertEquals(1L,  notif.deliveryCodeCalls.get(0).orderId());
    }

    @Test
    @DisplayName("READY_FOR_PICKUP order → code generated successfully")
    void readyForPickupOrder_codeGenerated() {
        CapturingSaveDeliveryCodePort save  = new CapturingSaveDeliveryCodePort();
        CapturingNotificationPort     notif = new CapturingNotificationPort();
        GenerateDeliveryCodeService   svc   = service(
                order(OrderStatus.READY_FOR_PICKUP, DeliveryType.PICKPOINT), save, notif);

        DeliveryCode result = svc.execute(1L);

        assertNotNull(result);
        assertTrue(result.getCode().matches("[0-9]{6}"));
        assertEquals(1, save.saveCallCount);
        assertEquals(1, notif.deliveryCodeCalls.size());
    }

    @Test
    @DisplayName("PENDING order → throws IllegalStateException")
    void pendingOrder_throwsIllegalState() {
        CapturingSaveDeliveryCodePort save = new CapturingSaveDeliveryCodePort();
        GenerateDeliveryCodeService   svc  = service(
                order(OrderStatus.PENDING, DeliveryType.HOME), save, new CapturingNotificationPort());

        assertThrows(IllegalStateException.class, () -> svc.execute(1L));
        assertEquals(0, save.saveCallCount);
    }

    @Test
    @DisplayName("Order not found → throws ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFoundException() {
        GenerateDeliveryCodeService svc = new GenerateDeliveryCodeService(
                orderPort(null), userPort(CUSTOMER),
                new CapturingSaveDeliveryCodePort(), new CapturingNotificationPort(), 72);

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L));
    }

    @Test
    @DisplayName("Existing active code is invalidated before generating new one")
    void invalidateCalledBeforeSave() {
        List<String> callOrder = new ArrayList<>();
        CapturingSaveDeliveryCodePort save = new CapturingSaveDeliveryCodePort() {
            @Override public void invalidateAllForOrder(Long orderId) {
                callOrder.add("invalidate");
                super.invalidateAllForOrder(orderId);
            }
            @Override public DeliveryCode save(DeliveryCode code) {
                callOrder.add("save");
                return super.save(code);
            }
        };
        GenerateDeliveryCodeService svc = service(
                order(OrderStatus.SHIPPED, DeliveryType.HOME), save, new CapturingNotificationPort());

        svc.execute(1L);

        assertEquals(List.of("invalidate", "save"), callOrder);
    }
}
