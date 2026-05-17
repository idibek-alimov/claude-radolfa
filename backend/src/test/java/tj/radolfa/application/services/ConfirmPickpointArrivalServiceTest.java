package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.ConfirmPickpointArrivalUseCase;
import tj.radolfa.application.ports.in.order.GenerateDeliveryCodeUseCase;
import tj.radolfa.application.ports.out.DeliveryEventPublisher;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
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

class ConfirmPickpointArrivalServiceTest {

    static final Long PICKPOINT_ID = 5L;

    // ── Factories ────────────────────────────────────────────────────────────

    static Order shippedPickpointOrder() {
        return new Order.Builder()
                .id(1L).userId(10L)
                .status(OrderStatus.SHIPPED)
                .deliveryType(DeliveryType.PICKPOINT)
                .pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(100)))
                .createdAt(Instant.now())
                .build();
    }

    static User staffUser(Long pickpointId) {
        return new User(99L, new PhoneNumber("+992000000001"), UserRole.PICKPOINT_STAFF,
                "Staff Name", null, null, true, 1L,
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

    static class CapturingSaveOrderPort implements SaveOrderPort {
        final List<Order> saved = new ArrayList<>();
        @Override public Order save(Order order) { saved.add(order); return order; }
        Order last() { return saved.get(saved.size() - 1); }
    }

    static class CapturingGenerateCodeUseCase implements GenerateDeliveryCodeUseCase {
        final List<Long> calledForOrders = new ArrayList<>();
        @Override public DeliveryCode execute(Long orderId) { calledForOrders.add(orderId); return null; }
    }

    static class CapturingEventPublisher implements DeliveryEventPublisher {
        final List<Long[]> pickpointEvents = new ArrayList<>();
        @Override public void publishNewOrderAtPickpoint(Long pickpointId, Long orderId) {
            pickpointEvents.add(new Long[]{pickpointId, orderId});
        }
        @Override public void publishOrderCancelledToCourier(Long c, Long o) {}
        @Override public void publishOrderAssignedToCourier(Long c, Long o) {}
        @Override public void publishOrderCancelledAtPickpoint(Long p, Long o) {}
        @Override public void publishDeliveryRetryLimitReached(Long o, Long c) {}
    }

    static ConfirmPickpointArrivalService service(Order order, User staff,
                                                   CapturingSaveOrderPort saveOrder,
                                                   CapturingGenerateCodeUseCase genCode,
                                                   CapturingEventPublisher events) {
        return new ConfirmPickpointArrivalService(
                orderPort(order), saveOrder, userPort(staff), genCode, events);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid staff at correct pickpoint + SHIPPED PICKPOINT order → READY_FOR_PICKUP, code generated, event published")
    void validArrival_transitionsAndNotifies() {
        var saveOrder = new CapturingSaveOrderPort();
        var genCode   = new CapturingGenerateCodeUseCase();
        var events    = new CapturingEventPublisher();
        Order order   = shippedPickpointOrder();
        User  staff   = staffUser(PICKPOINT_ID);

        service(order, staff, saveOrder, genCode, events).execute(1L, 99L);

        assertEquals(OrderStatus.READY_FOR_PICKUP, saveOrder.last().status());
        assertNotNull(saveOrder.last().readyForPickupAt());
        assertEquals(1, genCode.calledForOrders.size());
        assertEquals(1, events.pickpointEvents.size());
        assertEquals(PICKPOINT_ID, events.pickpointEvents.get(0)[0]);
    }

    @Test
    @DisplayName("Staff pickpointId doesn't match order pickpointId → PickpointAccessDeniedException")
    void wrongPickpoint_throwsAccessDenied() {
        Order order = shippedPickpointOrder();
        User  staff = staffUser(99L); // different pickpoint

        assertThrows(PickpointAccessDeniedException.class,
                () -> service(order, staff, new CapturingSaveOrderPort(),
                        new CapturingGenerateCodeUseCase(), new CapturingEventPublisher())
                        .execute(1L, 99L));
    }

    @Test
    @DisplayName("Staff has null pickpointId → PickpointAccessDeniedException")
    void nullPickpoint_throwsAccessDenied() {
        Order order = shippedPickpointOrder();
        User  staff = staffUser(null);

        assertThrows(PickpointAccessDeniedException.class,
                () -> service(order, staff, new CapturingSaveOrderPort(),
                        new CapturingGenerateCodeUseCase(), new CapturingEventPublisher())
                        .execute(1L, 99L));
    }

    @Test
    @DisplayName("Order not SHIPPED → IllegalStateException")
    void notShipped_throwsIllegalState() {
        Order order = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.PAID)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(100))).createdAt(Instant.now())
                .build();
        User staff = staffUser(PICKPOINT_ID);

        assertThrows(IllegalStateException.class,
                () -> service(order, staff, new CapturingSaveOrderPort(),
                        new CapturingGenerateCodeUseCase(), new CapturingEventPublisher())
                        .execute(1L, 99L));
    }

    @Test
    @DisplayName("Order is HOME delivery → IllegalStateException")
    void homeDelivery_throwsIllegalState() {
        Order order = new Order.Builder()
                .id(1L).userId(10L).status(OrderStatus.SHIPPED)
                .deliveryType(DeliveryType.HOME).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(100))).createdAt(Instant.now())
                .build();
        User staff = staffUser(PICKPOINT_ID);

        assertThrows(IllegalStateException.class,
                () -> service(order, staff, new CapturingSaveOrderPort(),
                        new CapturingGenerateCodeUseCase(), new CapturingEventPublisher())
                        .execute(1L, 99L));
    }

    @Test
    @DisplayName("Order not found → ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFound() {
        User staff = staffUser(PICKPOINT_ID);

        assertThrows(ResourceNotFoundException.class,
                () -> service(null, staff, new CapturingSaveOrderPort(),
                        new CapturingGenerateCodeUseCase(), new CapturingEventPublisher())
                        .execute(99L, 99L));
    }
}
