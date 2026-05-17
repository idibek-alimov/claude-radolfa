package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

class ConfirmReturnedToWarehouseServiceTest {

    static final Long PICKPOINT_ID = 5L;
    static final Long ORDER_ID     = 1L;

    // ── Factories ────────────────────────────────────────────────────────────

    static Order returnInitiatedOrder() {
        return new Order.Builder()
                .id(ORDER_ID).userId(10L)
                .status(OrderStatus.RETURN_INITIATED)
                .deliveryType(DeliveryType.PICKPOINT)
                .pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(150)))
                .createdAt(Instant.now())
                .build();
    }

    static User staffUser(Long pickpointId) {
        return new User(99L, new PhoneNumber("+992000000088"), UserRole.PICKPOINT_STAFF,
                "Staff", null, null, true, 1L,
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

    static ConfirmReturnedToWarehouseService service(Order order, User staff,
                                                      CapturingSaveOrderPort saveOrder) {
        return new ConfirmReturnedToWarehouseService(orderPort(order), saveOrder, userPort(staff));
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid staff at correct pickpoint + RETURN_INITIATED → RETURNED_TO_WAREHOUSE, timestamp set")
    void validStaff_transitionsOrder() {
        var saveOrder = new CapturingSaveOrderPort();
        service(returnInitiatedOrder(), staffUser(PICKPOINT_ID), saveOrder).execute(ORDER_ID, 99L);

        assertEquals(OrderStatus.RETURNED_TO_WAREHOUSE, saveOrder.last().status());
        assertNotNull(saveOrder.last().returnedToWarehouseAt());
    }

    @Test
    @DisplayName("Staff at wrong pickpoint → PickpointAccessDeniedException")
    void wrongPickpoint_throwsAccessDenied() {
        assertThrows(PickpointAccessDeniedException.class,
                () -> service(returnInitiatedOrder(), staffUser(99L),
                        new CapturingSaveOrderPort()).execute(ORDER_ID, 99L));
    }

    @Test
    @DisplayName("Staff with null pickpointId → PickpointAccessDeniedException")
    void nullPickpoint_throwsAccessDenied() {
        assertThrows(PickpointAccessDeniedException.class,
                () -> service(returnInitiatedOrder(), staffUser(null),
                        new CapturingSaveOrderPort()).execute(ORDER_ID, 99L));
    }

    @Test
    @DisplayName("Order not RETURN_INITIATED → IllegalStateException")
    void orderNotReturnInitiated_throwsIllegalState() {
        Order readyOrder = new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.READY_FOR_PICKUP)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(150))).createdAt(Instant.now())
                .build();
        assertThrows(IllegalStateException.class,
                () -> service(readyOrder, staffUser(PICKPOINT_ID),
                        new CapturingSaveOrderPort()).execute(ORDER_ID, 99L));
    }

    @Test
    @DisplayName("Order not found → ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(null, staffUser(PICKPOINT_ID),
                        new CapturingSaveOrderPort()).execute(999L, 99L));
    }
}
