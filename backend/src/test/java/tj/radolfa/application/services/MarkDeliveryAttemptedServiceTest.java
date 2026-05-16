package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.MarkDeliveryAttemptedUseCase.Command;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.CourierAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryAttemptReason;
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

class MarkDeliveryAttemptedServiceTest {

    static final long COURIER_ID = 99L;
    static final int  MAX        = 3;

    static Order outForDeliveryOrder(int existingAttempts) {
        return new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.OUT_FOR_DELIVERY)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(COURIER_ID).deliveryAttemptCount(existingAttempts)
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
            @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
            @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
        };
    }

    static MarkDeliveryAttemptedService service(Order order, CapturingSaveOrderPort save) {
        return new MarkDeliveryAttemptedService(orderPort(order), save,
                new OrderNotificationService(silentPort()), MAX);
    }

    @Test
    @DisplayName("Valid attempt → status DELIVERY_ATTEMPTED, count incremented, reason + photo persisted")
    void validAttempt_statusAndFieldsUpdated() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        MarkDeliveryAttemptedService svc = service(outForDeliveryOrder(0), save);

        Command cmd = new Command(1L, COURIER_ID, DeliveryAttemptReason.NO_ANSWER, "http://s3/photo.jpg");
        Instant before = Instant.now();
        svc.execute(cmd);
        Instant after = Instant.now();

        Order saved = save.last();
        assertEquals(OrderStatus.DELIVERY_ATTEMPTED, saved.status());
        assertEquals(1, saved.deliveryAttemptCount());
        assertEquals(DeliveryAttemptReason.NO_ANSWER, saved.deliveryAttemptReason());
        assertEquals("http://s3/photo.jpg", saved.deliveryPhotoUrl());
        assertNotNull(saved.deliveryAttemptedAt());
        assertFalse(saved.deliveryAttemptedAt().isBefore(before));
        assertFalse(saved.deliveryAttemptedAt().isAfter(after));
    }

    @Test
    @DisplayName("Photo URL is optional (null → still succeeds)")
    void nullPhotoUrl_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        MarkDeliveryAttemptedService svc = service(outForDeliveryOrder(0), save);

        svc.execute(new Command(1L, COURIER_ID, DeliveryAttemptReason.OTHER, null));

        assertNull(save.last().deliveryPhotoUrl());
        assertEquals(OrderStatus.DELIVERY_ATTEMPTED, save.last().status());
    }

    @Test
    @DisplayName("Wrong courierId → CourierAccessDeniedException")
    void wrongCourier_throwsAccessDenied() {
        MarkDeliveryAttemptedService svc = service(outForDeliveryOrder(0), new CapturingSaveOrderPort());

        assertThrows(CourierAccessDeniedException.class,
                () -> svc.execute(new Command(1L, 42L, DeliveryAttemptReason.NO_ANSWER, null)));
    }

    @Test
    @DisplayName("Wrong status (SHIPPED) → IllegalStateException")
    void wrongStatus_throwsIllegalState() {
        Order shippedOrder = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.SHIPPED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(COURIER_ID)
                .build();

        MarkDeliveryAttemptedService svc = service(shippedOrder, new CapturingSaveOrderPort());

        assertThrows(IllegalStateException.class,
                () -> svc.execute(new Command(1L, COURIER_ID, DeliveryAttemptReason.NO_ANSWER, null)));
    }

    @Test
    @DisplayName("Attempt count reaches max → order saved with incremented count")
    void attemptsAtMax_orderSavedWithMaxCount() {
        // existing count = MAX - 1, so after increment we hit MAX
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        MarkDeliveryAttemptedService svc = service(outForDeliveryOrder(MAX - 1), save);

        svc.execute(new Command(1L, COURIER_ID, DeliveryAttemptReason.NO_ANSWER, null));

        assertEquals(MAX, save.last().deliveryAttemptCount());
        assertEquals(OrderStatus.DELIVERY_ATTEMPTED, save.last().status());
    }

    @Test
    @DisplayName("Order not found → throws ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFoundException() {
        MarkDeliveryAttemptedService svc = service(null, new CapturingSaveOrderPort());

        assertThrows(ResourceNotFoundException.class,
                () -> svc.execute(new Command(999L, COURIER_ID, DeliveryAttemptReason.NO_ANSWER, null)));
    }
}
