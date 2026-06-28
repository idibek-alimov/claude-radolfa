package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase.Command;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.NotificationPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateOrderStatusServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static Order homeOrder(OrderStatus status) {
        return new Order.Builder()
                .id(1L).userId(10L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("123 Main St")
                .preferredTimeWindow("MORNING")
                .build();
    }

    static Order homeClaimedOrder() {
        return new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.CLAIMED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("123 Main St")
                .preferredTimeWindow("MORNING")
                .courierId(99L).claimedAt(Instant.now())
                .build();
    }

    static Order homeShippedOrder() {
        return new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.SHIPPED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("123 Main St")
                .preferredTimeWindow("MORNING")
                .courierId(99L).trackingNumber("TST123")
                .estimatedDeliveryDate(LocalDate.of(2026, 6, 1)).shippedAt(Instant.now())
                .build();
    }

    static Order pickpointOrder(OrderStatus status) {
        return new Order.Builder()
                .id(2L).userId(10L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(99L)
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
        @Override public Order save(Order order) { saved.add(order); return order; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class CountingNotificationPort implements NotificationPort {
        int confirmCount = 0;
        int updateCount  = 0;
        OrderStatus lastStatus;
        @Override public void sendOrderConfirmation(Long u, Long o) { confirmCount++; }
        @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) { updateCount++; lastStatus = s; }
        @Override public void sendReviewApprovedNotification(Long u, Long r) {}
        @Override public void sendReviewReplyNotification(Long u, Long r) {}
        @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
    }

    static NotificationPort silentPort() {
        return new NotificationPort() {
            @Override public void sendOrderConfirmation(Long u, Long o) {}
            @Override public void sendOrderStatusUpdate(Long u, Long o, OrderStatus s) {}
            @Override public void sendReviewApprovedNotification(Long u, Long r) {}
            @Override public void sendReviewReplyNotification(Long u, Long r) {}
            @Override public void sendDeliveryCode(Long u, Long o, String c, java.time.Instant e) {}
        @Override public void sendPickpointExpiryWarning(Long u, Long o, int d) {}
        @Override public void sendPickpointOrderExpiredCancellation(Long u, Long o) {}
        };
    }

    static class FakeGenerateDeliveryCodeUseCase implements GenerateDeliveryCodeUseCase {
        Long lastOrderId;
        @Override public DeliveryCode execute(Long orderId) { this.lastOrderId = orderId; return null; }
    }

    static final tj.radolfa.application.ports.out.DeliveryEventPublisher NO_DELIVERY_EVENTS =
            new tj.radolfa.application.ports.out.DeliveryEventPublisher() {
                @Override public void publishOrderCancelledToCourier(Long c, Long o) {}
                @Override public void publishOrderAssignedToCourier(Long c, Long o) {}
                @Override public void publishNewOrderAtPickpoint(Long p, Long o) {}
                @Override public void publishOrderCancelledAtPickpoint(Long p, Long o) {}
                @Override public void publishDeliveryRetryLimitReached(Long o, Long c) {}
            };

    static class FakeSaveOrderStatusChangePort implements tj.radolfa.application.ports.out.SaveOrderStatusChangePort {
        final List<tj.radolfa.domain.model.OrderStatusChange> appended = new ArrayList<>();
        @Override
        public tj.radolfa.domain.model.OrderStatusChange append(tj.radolfa.domain.model.OrderStatusChange c) {
            appended.add(c); return c;
        }
    }

    static OrderStatusChangeRecorder noopRecorder() {
        return new OrderStatusChangeRecorder(new FakeSaveOrderStatusChangePort());
    }

    static UpdateOrderStatusService service(Order order, CapturingSaveOrderPort save) {
        return new UpdateOrderStatusService(orderPort(order), save,
                new OrderNotificationService(silentPort()), new FakeGenerateDeliveryCodeUseCase(),
                NO_DELIVERY_EVENTS, noopRecorder());
    }

    static UpdateOrderStatusService service(Order order, CapturingSaveOrderPort save, NotificationPort notifPort) {
        return new UpdateOrderStatusService(orderPort(order), save,
                new OrderNotificationService(notifPort), new FakeGenerateDeliveryCodeUseCase(),
                NO_DELIVERY_EVENTS, noopRecorder());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HOME PICKED→SHIPPED is now rejected (couriers claim instead)")
    void homePickedToShipped_nowRejected() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PICKED), new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.SHIPPED, 99L, "TST123",
                        LocalDate.of(2026, 6, 1), null)));
    }

    @Test
    @DisplayName("HOME CLAIMED→PICKED (admin unclaim) succeeds; courierId and claimedAt are cleared")
    void homeUnclaimSucceeds_courierAndClaimedAtCleared() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeClaimedOrder(), save);

        svc.execute(new Command(1L, OrderStatus.PICKED, null, null, null, null));

        Order saved = save.last();
        assertEquals(OrderStatus.PICKED, saved.status());
        assertNull(saved.courierId(),  "courierId must be null after unclaim");
        assertNull(saved.claimedAt(),  "claimedAt must be null after unclaim");
    }

    @Test
    @DisplayName("PICKPOINT PICKED→SHIPPED succeeds (staff-driven arrival flow)")
    void pickpointPickedToShipped_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(pickpointOrder(OrderStatus.PICKED), save);

        svc.execute(new Command(2L, OrderStatus.SHIPPED, null, null, null, null));

        assertEquals(OrderStatus.SHIPPED, save.last().status());
    }

    @Test
    @DisplayName("PICKPOINT PICKED→READY_FOR_PICKUP without courierId succeeds (no courier required)")
    void pickpointTransitionToReadyForPickup_noCourierRequired() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(pickpointOrder(OrderStatus.PICKED), save);

        svc.execute(new Command(2L, OrderStatus.READY_FOR_PICKUP, null, null, null, null));

        assertEquals(OrderStatus.READY_FOR_PICKUP, save.last().status());
        assertNull(save.last().courierId());
    }

    @Test
    @DisplayName("PENDING→PAID with stray courierId: succeeds, courier fields NOT written")
    void pendingToPaid_courierFieldsIgnored() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PENDING), save);

        svc.execute(new Command(1L, OrderStatus.PAID, 99L, "X", LocalDate.now(), null));

        Order saved = save.last();
        assertEquals(OrderStatus.PAID, saved.status());
        assertNull(saved.courierId());
        assertNull(saved.trackingNumber());
        assertNull(saved.estimatedDeliveryDate());
    }

    @Test
    @DisplayName("SHIPPED→DELIVERED preserves existing courier fields")
    void shippedToDelivered_courierFieldsCarriedThrough() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeShippedOrder(), save);

        svc.execute(new Command(1L, OrderStatus.DELIVERED, null, null, null, null));

        Order saved = save.last();
        assertEquals(OrderStatus.DELIVERED, saved.status());
        assertEquals(99L, saved.courierId());
        assertEquals("TST123", saved.trackingNumber());
        assertEquals(LocalDate.of(2026, 6, 1), saved.estimatedDeliveryDate());
    }

    @Test
    @DisplayName("Invalid transition PAID→PENDING throws")
    void invalidTransition_throws() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.PENDING, null, null, null, null)));
    }

    @Test
    @DisplayName("PICKPOINT PICKED→READY_FOR_PICKUP succeeds")
    void pickpointPickedToReadyForPickup_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(pickpointOrder(OrderStatus.PICKED), save);

        svc.execute(new Command(2L, OrderStatus.READY_FOR_PICKUP, null, null, null, null));

        assertEquals(OrderStatus.READY_FOR_PICKUP, save.last().status());
    }

    @Test
    @DisplayName("PICKPOINT READY_FOR_PICKUP→DELIVERED succeeds")
    void pickpointReadyForPickupToDelivered_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(pickpointOrder(OrderStatus.READY_FOR_PICKUP), save);

        svc.execute(new Command(2L, OrderStatus.DELIVERED, null, null, null, null));

        assertEquals(OrderStatus.DELIVERED, save.last().status());
    }

    @Test
    @DisplayName("HOME PAID→READY_FOR_PICKUP throws (cross-track forbidden)")
    void homePaidToReadyForPickup_throws() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.READY_FOR_PICKUP, null, null, null, null)));
    }

    @Test
    @DisplayName("HOME SHIPPED→DELIVERED still works (regression — in-flight orders)")
    void homeShippedToDelivered_regression() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeShippedOrder(), save);

        svc.execute(new Command(1L, OrderStatus.DELIVERED, null, null, null, null));

        assertEquals(OrderStatus.DELIVERED, save.last().status());
    }

    @Test
    @DisplayName("PICKPOINT PICKED→SHIPPED fires exactly one SHIPPED notification")
    void notification_firedOnSuccess() {
        CountingNotificationPort port = new CountingNotificationPort();
        CapturingSaveOrderPort save   = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc  = service(pickpointOrder(OrderStatus.PICKED), save, port);

        svc.execute(new Command(2L, OrderStatus.SHIPPED, null, null, null, null));

        assertEquals(1, port.updateCount);
        assertEquals(OrderStatus.SHIPPED, port.lastStatus);
    }

    @Test
    @DisplayName("Invalid transition does not fire any notification")
    void notification_notFiredOnInvalidTransition() {
        CountingNotificationPort port = new CountingNotificationPort();
        CapturingSaveOrderPort save   = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc  = service(homeOrder(OrderStatus.PAID), save, port);

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.PENDING, null, null, null, null)));

        assertEquals(0, port.confirmCount + port.updateCount);
    }

    @Test
    @DisplayName("PICKPOINT PICKED→SHIPPED sets shippedAt; deliveredAt remains null")
    void pickpointPickedToShipped_setsShippedAt() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(pickpointOrder(OrderStatus.PICKED), save);

        Instant before = Instant.now();
        svc.execute(new Command(2L, OrderStatus.SHIPPED, null, null, null, null));
        Instant after = Instant.now();

        Order saved = save.last();
        assertNotNull(saved.shippedAt());
        assertFalse(saved.shippedAt().isBefore(before));
        assertFalse(saved.shippedAt().isAfter(after));
        assertNull(saved.deliveredAt());
        assertNull(saved.cancelledAt());
    }

    @Test
    @DisplayName("SHIPPED→DELIVERED sets deliveredAt; shippedAt carried through")
    void shippedToDelivered_setsDeliveredAt() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeShippedOrder(), save);

        Instant before = Instant.now();
        svc.execute(new Command(1L, OrderStatus.DELIVERED, null, null, null, null));
        Instant after = Instant.now();

        Order saved = save.last();
        assertNotNull(saved.deliveredAt());
        assertFalse(saved.deliveredAt().isBefore(before));
        assertFalse(saved.deliveredAt().isAfter(after));
        assertNotNull(saved.shippedAt());
        assertNull(saved.cancelledAt());
    }

    @Test
    @DisplayName("Status update preserves all unrelated fields unchanged")
    void statusUpdate_preservesUnrelatedFields() {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        // Use pickpoint PICKED → SHIPPED for this coverage test (HOME PICKED→SHIPPED now invalid)
        Order pristine = new Order.Builder()
                .id(42L).userId(7L).externalOrderId("EXT-XYZ")
                .status(OrderStatus.PICKED).totalAmount(new Money(new BigDecimal("123.45")))
                .items(List.of()).createdAt(created)
                .loyaltyPointsRedeemed(150).loyaltyPointsAwarded(30)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(99L)
                .build();

        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = new UpdateOrderStatusService(
                orderPort(pristine), save,
                new OrderNotificationService(silentPort()), new FakeGenerateDeliveryCodeUseCase(),
                NO_DELIVERY_EVENTS, noopRecorder());

        svc.execute(new Command(42L, OrderStatus.SHIPPED, null, null, null, null));

        Order out = save.last();
        assertEquals(42L,              out.id());
        assertEquals(7L,               out.userId());
        assertEquals("EXT-XYZ",        out.externalOrderId());
        assertEquals(new BigDecimal("123.45"), out.totalAmount().amount());
        assertEquals(created,          out.createdAt());
        assertEquals(150,              out.loyaltyPointsRedeemed());
        assertEquals(30,               out.loyaltyPointsAwarded());
        assertEquals(DeliveryType.PICKPOINT, out.deliveryType());
        assertEquals(99L,              out.pickpointId());
        assertNull(out.cancelledAt());
        assertNull(out.refundedAt());
        assertEquals(OrderStatus.SHIPPED, out.status());
    }

    @Test
    @DisplayName("HOME PAID→SHIPPED now throws (pick gate enforced)")
    void homePaidToShipped_throws() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.SHIPPED, 99L, "T1", null, null)));
    }

    @Test
    @DisplayName("PAID→PICKED succeeds (auto-triggered by CompletePickSessionService once all units are scanned)")
    void paidToPicked_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.PAID), save);

        svc.execute(new Command(1L, OrderStatus.PICKED, null, null, null, null));

        assertEquals(OrderStatus.PICKED, save.last().status());
    }

    @Test
    @DisplayName("AWAITING_COD→PICKED succeeds, same as PAID")
    void awaitingCodToPicked_succeeds() {
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.AWAITING_COD), save);

        svc.execute(new Command(1L, OrderStatus.PICKED, null, null, null, null));

        assertEquals(OrderStatus.PICKED, save.last().status());
    }

    @Test
    @DisplayName("AWAITING_COD→SHIPPED (skipping PICKED) still rejected")
    void awaitingCodToShipped_throws() {
        UpdateOrderStatusService svc = service(homeOrder(OrderStatus.AWAITING_COD), new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.SHIPPED, 99L, "T1", null, null)));
    }

    @Test
    @DisplayName("Admin DELIVERY_ATTEMPTED → SHIPPED is no longer valid (courier self-retry via RetryDeliveryService)")
    void deliveryAttemptedToShipped_nowRejected() {
        Order attemptedOrder = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.DELIVERY_ATTEMPTED)
                .totalAmount(new Money(BigDecimal.valueOf(500))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME).deliveryAddress("Addr")
                .courierId(99L).deliveryAttemptCount(1)
                .build();

        UpdateOrderStatusService svc = service(attemptedOrder, new CapturingSaveOrderPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.SHIPPED, 99L, null, null, null)));
    }

    @Test
    @DisplayName("Successful transition records one status-change row with correct from/to/actor")
    void successfulTransition_recordsLedgerRow() {
        FakeSaveOrderStatusChangePort port = new FakeSaveOrderStatusChangePort();
        OrderStatusChangeRecorder recorder = new OrderStatusChangeRecorder(port);
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = new UpdateOrderStatusService(
                orderPort(pickpointOrder(OrderStatus.PICKED)), save,
                new OrderNotificationService(silentPort()), new FakeGenerateDeliveryCodeUseCase(),
                NO_DELIVERY_EVENTS, recorder);

        svc.execute(new Command(2L, OrderStatus.SHIPPED, null, null, null, 42L));

        assertEquals(1, port.appended.size());
        var row = port.appended.get(0);
        assertEquals(2L, row.orderId());
        assertEquals(OrderStatus.PICKED, row.statusFrom());
        assertEquals(OrderStatus.SHIPPED, row.statusTo());
        assertEquals(42L, row.actorUserId());
    }

    @Test
    @DisplayName("Failed transition (invalid) records no ledger row")
    void failedTransition_recordsNoLedgerRow() {
        FakeSaveOrderStatusChangePort port = new FakeSaveOrderStatusChangePort();
        OrderStatusChangeRecorder recorder = new OrderStatusChangeRecorder(port);
        UpdateOrderStatusService svc = new UpdateOrderStatusService(
                orderPort(homeOrder(OrderStatus.PAID)), new CapturingSaveOrderPort(),
                new OrderNotificationService(silentPort()), new FakeGenerateDeliveryCodeUseCase(),
                NO_DELIVERY_EVENTS, recorder);

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(1L, OrderStatus.PENDING, null, null, null, 42L)));

        assertTrue(port.appended.isEmpty());
    }

    @Test
    @DisplayName("System transition (null actor) records null actorUserId in ledger")
    void systemTransition_recordsNullActor() {
        FakeSaveOrderStatusChangePort port = new FakeSaveOrderStatusChangePort();
        OrderStatusChangeRecorder recorder = new OrderStatusChangeRecorder(port);
        CapturingSaveOrderPort save = new CapturingSaveOrderPort();
        UpdateOrderStatusService svc = new UpdateOrderStatusService(
                orderPort(homeOrder(OrderStatus.PENDING)), save,
                new OrderNotificationService(silentPort()), new FakeGenerateDeliveryCodeUseCase(),
                NO_DELIVERY_EVENTS, recorder);

        svc.execute(new Command(1L, OrderStatus.PAID, null, null, null, null));

        assertEquals(1, port.appended.size());
        assertNull(port.appended.get(0).actorUserId());
    }
}
