package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.DeliveryEventPublisher;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.domain.exception.OrderRecallNotAllowedException;
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

class RequestOrderRecallServiceTest {

    static final Long ADMIN_ID     = 99L;
    static final Long COURIER_ID   = 55L;
    static final Long PICKPOINT_ID = 5L;
    static final String REASON     = "Customer requested cancellation";

    // ── Fakes ────────────────────────────────────────────────────────────────

    static Order order(OrderStatus status) {
        return new Order.Builder()
                .id(1L).userId(10L).status(status)
                .courierId(status == OrderStatus.READY_FOR_PICKUP ? null : COURIER_ID)
                .pickpointId(status == OrderStatus.READY_FOR_PICKUP ? PICKPOINT_ID : null)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .deliveryType(status == OrderStatus.READY_FOR_PICKUP ? DeliveryType.PICKPOINT : DeliveryType.HOME)
                .build();
    }

    static Order orderWithoutCourier(OrderStatus status) {
        return new Order.Builder()
                .id(1L).userId(10L).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(300))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME)
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

    static class FakeDeliveryEventPublisher implements DeliveryEventPublisher {
        final List<String> courierRecalls   = new ArrayList<>();
        final List<String> pickpointRecalls = new ArrayList<>();
        @Override public void publishOrderCancelledToCourier(Long c, Long o) {}
        @Override public void publishOrderAssignedToCourier(Long c, Long o) {}
        @Override public void publishNewOrderAtPickpoint(Long p, Long o) {}
        @Override public void publishOrderCancelledAtPickpoint(Long p, Long o) {}
        @Override public void publishDeliveryRetryLimitReached(Long o, Long c) {}
        @Override public void publishOrderRecallToCourier(Long c, Long o) {
            courierRecalls.add(c + ":" + o);
        }
        @Override public void publishOrderRecallToPickpoint(Long p, Long o) {
            pickpointRecalls.add(p + ":" + o);
        }
    }

    static RequestOrderRecallService service(Order order,
                                              CapturingSaveOrderPort save,
                                              FakeDeliveryEventPublisher publisher) {
        return new RequestOrderRecallService(orderPort(order), save, publisher);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHIPPED order → RECALL_REQUESTED, recall fields set, courier WS event fired")
    void shippedOrder_transitionsToRecallAndNotifiesCourier() {
        var save      = new CapturingSaveOrderPort();
        var publisher = new FakeDeliveryEventPublisher();
        service(order(OrderStatus.SHIPPED), save, publisher).execute(1L, ADMIN_ID, REASON);

        assertEquals(OrderStatus.RECALL_REQUESTED, save.last().status());
        assertNotNull(save.last().recallRequestedAt());
        assertEquals(ADMIN_ID, save.last().recallRequestedByUserId());
        assertEquals(REASON, save.last().recallReason());
        assertEquals(1, publisher.courierRecalls.size());
        assertTrue(publisher.courierRecalls.get(0).contains(COURIER_ID.toString()));
        assertTrue(publisher.pickpointRecalls.isEmpty());
    }

    @Test
    @DisplayName("OUT_FOR_DELIVERY order → courier WS event fired")
    void outForDelivery_notifiesCourier() {
        var save      = new CapturingSaveOrderPort();
        var publisher = new FakeDeliveryEventPublisher();
        service(order(OrderStatus.OUT_FOR_DELIVERY), save, publisher).execute(1L, ADMIN_ID, REASON);

        assertEquals(OrderStatus.RECALL_REQUESTED, save.last().status());
        assertEquals(1, publisher.courierRecalls.size());
    }

    @Test
    @DisplayName("READY_FOR_PICKUP order → pickpoint WS event fired; courier event NOT fired")
    void readyForPickup_notifiesPickpoint() {
        var save      = new CapturingSaveOrderPort();
        var publisher = new FakeDeliveryEventPublisher();
        service(order(OrderStatus.READY_FOR_PICKUP), save, publisher).execute(1L, ADMIN_ID, REASON);

        assertEquals(OrderStatus.RECALL_REQUESTED, save.last().status());
        assertEquals(1, publisher.pickpointRecalls.size());
        assertTrue(publisher.courierRecalls.isEmpty());
    }

    @Test
    @DisplayName("PENDING order → OrderRecallNotAllowedException")
    void pendingOrder_throwsRecallNotAllowed() {
        assertThrows(OrderRecallNotAllowedException.class,
                () -> service(order(OrderStatus.PENDING), new CapturingSaveOrderPort(),
                        new FakeDeliveryEventPublisher()).execute(1L, ADMIN_ID, REASON));
    }

    @Test
    @DisplayName("DELIVERED order → OrderRecallNotAllowedException")
    void deliveredOrder_throwsRecallNotAllowed() {
        assertThrows(OrderRecallNotAllowedException.class,
                () -> service(order(OrderStatus.DELIVERED), new CapturingSaveOrderPort(),
                        new FakeDeliveryEventPublisher()).execute(1L, ADMIN_ID, REASON));
    }

    @Test
    @DisplayName("RECALL_REQUESTED again → OrderRecallNotAllowedException (idempotency guard)")
    void alreadyRecalled_throwsRecallNotAllowed() {
        assertThrows(OrderRecallNotAllowedException.class,
                () -> service(order(OrderStatus.RECALL_REQUESTED), new CapturingSaveOrderPort(),
                        new FakeDeliveryEventPublisher()).execute(1L, ADMIN_ID, REASON));
    }

    @Test
    @DisplayName("SHIPPED order with no courier assigned → status changes but no WS event fired")
    void shippedNoCourier_noWsEvent() {
        var save      = new CapturingSaveOrderPort();
        var publisher = new FakeDeliveryEventPublisher();
        service(orderWithoutCourier(OrderStatus.SHIPPED), save, publisher).execute(1L, ADMIN_ID, REASON);

        assertEquals(OrderStatus.RECALL_REQUESTED, save.last().status());
        assertTrue(publisher.courierRecalls.isEmpty());
        assertTrue(publisher.pickpointRecalls.isEmpty());
    }
}
