package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.ConfirmWithDeliveryCodeUseCase.Command;
import tj.radolfa.application.ports.out.LoadDeliveryCodePort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveDeliveryCodePort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.DeliveryCodeAlreadyUsedException;
import tj.radolfa.domain.exception.DeliveryCodeAttemptsExhaustedException;
import tj.radolfa.domain.exception.DeliveryCodeExpiredException;
import tj.radolfa.domain.exception.DeliveryCodeMismatchException;
import tj.radolfa.domain.exception.DeliveryCodeNotFoundException;
import tj.radolfa.domain.model.DeliveryCode;
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

class ConfirmWithDeliveryCodeServiceTest {

    static final String VALID_CODE = "123456";

    // ── Fakes ────────────────────────────────────────────────────────────────

    static DeliveryCode activeCode() {
        return new DeliveryCode(1L, 1L, VALID_CODE,
                Instant.now().plusSeconds(3600), null, 0, Instant.now());
    }

    static DeliveryCode expiredCode() {
        return new DeliveryCode(1L, 1L, VALID_CODE,
                Instant.now().minusSeconds(1), null, 0, Instant.now());
    }

    static DeliveryCode usedCode() {
        DeliveryCode code = activeCode();
        code.markUsed();
        return code;
    }

    static DeliveryCode exhaustedCode(int maxAttempts) {
        return new DeliveryCode(1L, 1L, VALID_CODE,
                Instant.now().plusSeconds(3600), null, maxAttempts, Instant.now());
    }

    static Order order(OrderStatus status, DeliveryType type) {
        return new Order.Builder()
                .id(1L).userId(10L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(type).deliveryAddress("Addr")
                .build();
    }

    static LoadDeliveryCodePort codePort(DeliveryCode code) {
        return orderId -> code != null ? Optional.of(code) : Optional.empty();
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
        @Override public Order save(Order order) { saved.add(order); return order; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class CapturingSaveDeliveryCodePort implements SaveDeliveryCodePort {
        final List<DeliveryCode> saved = new ArrayList<>();
        @Override public DeliveryCode save(DeliveryCode code) { saved.add(code); return code; }
        @Override public void invalidateAllForOrder(Long orderId) {}
        DeliveryCode last() { return saved.get(saved.size() - 1); }
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

    static ConfirmWithDeliveryCodeService service(DeliveryCode code, Order order,
                                                   CapturingSaveDeliveryCodePort saveCode,
                                                   CapturingSaveOrderPort saveOrder) {
        return new ConfirmWithDeliveryCodeService(
                codePort(code), saveCode, orderPort(order), saveOrder,
                new OrderNotificationService(silentPort()), 5);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Correct code on SHIPPED order → DELIVERED, code marked used")
    void correctCodeOnShipped_orderDelivered() {
        CapturingSaveDeliveryCodePort saveCode  = new CapturingSaveDeliveryCodePort();
        CapturingSaveOrderPort        saveOrder = new CapturingSaveOrderPort();
        ConfirmWithDeliveryCodeService svc = service(
                activeCode(), order(OrderStatus.SHIPPED, DeliveryType.HOME), saveCode, saveOrder);

        svc.execute(new Command(1L, VALID_CODE));

        assertEquals(OrderStatus.DELIVERED, saveOrder.last().status());
        assertNotNull(saveOrder.last().deliveredAt());
        assertTrue(saveCode.last().isUsed());
    }

    @Test
    @DisplayName("Correct code on READY_FOR_PICKUP order → DELIVERED")
    void correctCodeOnReadyForPickup_orderDelivered() {
        CapturingSaveDeliveryCodePort saveCode  = new CapturingSaveDeliveryCodePort();
        CapturingSaveOrderPort        saveOrder = new CapturingSaveOrderPort();
        ConfirmWithDeliveryCodeService svc = service(
                activeCode(), order(OrderStatus.READY_FOR_PICKUP, DeliveryType.PICKPOINT), saveCode, saveOrder);

        svc.execute(new Command(1L, VALID_CODE));

        assertEquals(OrderStatus.DELIVERED, saveOrder.last().status());
    }

    @Test
    @DisplayName("Wrong code → DeliveryCodeMismatchException, attempt count incremented")
    void wrongCode_mismatchExceptionAndAttemptIncremented() {
        CapturingSaveDeliveryCodePort saveCode  = new CapturingSaveDeliveryCodePort();
        CapturingSaveOrderPort        saveOrder = new CapturingSaveOrderPort();
        ConfirmWithDeliveryCodeService svc = service(
                activeCode(), order(OrderStatus.SHIPPED, DeliveryType.HOME), saveCode, saveOrder);

        assertThrows(DeliveryCodeMismatchException.class,
                () -> svc.execute(new Command(1L, "000000")));

        assertEquals(1, saveCode.last().getAttemptCount());
        assertTrue(saveOrder.saved.isEmpty());
    }

    @Test
    @DisplayName("Expired code → DeliveryCodeExpiredException")
    void expiredCode_throwsExpiredException() {
        ConfirmWithDeliveryCodeService svc = service(
                expiredCode(), order(OrderStatus.SHIPPED, DeliveryType.HOME),
                new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort());

        assertThrows(DeliveryCodeExpiredException.class,
                () -> svc.execute(new Command(1L, VALID_CODE)));
    }

    @Test
    @DisplayName("Already-used code → DeliveryCodeAlreadyUsedException")
    void alreadyUsedCode_throwsAlreadyUsedException() {
        ConfirmWithDeliveryCodeService svc = service(
                usedCode(), order(OrderStatus.SHIPPED, DeliveryType.HOME),
                new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort());

        assertThrows(DeliveryCodeAlreadyUsedException.class,
                () -> svc.execute(new Command(1L, VALID_CODE)));
    }

    @Test
    @DisplayName("Attempts exhausted → DeliveryCodeAttemptsExhaustedException without incrementing further")
    void attemptsExhausted_throwsAndDoesNotIncrement() {
        CapturingSaveDeliveryCodePort saveCode = new CapturingSaveDeliveryCodePort();
        ConfirmWithDeliveryCodeService svc = service(
                exhaustedCode(5), order(OrderStatus.SHIPPED, DeliveryType.HOME),
                saveCode, new CapturingSaveOrderPort());

        assertThrows(DeliveryCodeAttemptsExhaustedException.class,
                () -> svc.execute(new Command(1L, VALID_CODE)));

        assertTrue(saveCode.saved.isEmpty());
    }

    @Test
    @DisplayName("Code not found → DeliveryCodeNotFoundException")
    void codeNotFound_throwsNotFoundException() {
        ConfirmWithDeliveryCodeService svc = service(
                null, order(OrderStatus.SHIPPED, DeliveryType.HOME),
                new CapturingSaveDeliveryCodePort(), new CapturingSaveOrderPort());

        assertThrows(DeliveryCodeNotFoundException.class,
                () -> svc.execute(new Command(1L, VALID_CODE)));
    }
}
