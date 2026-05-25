package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.exception.OrderNotAtPickpointException;
import tj.radolfa.domain.exception.OrderNotDeliveredException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LookUpOrderForReturnServiceTest {

    static final Long PICKPOINT_ID = 5L;
    static final Long ORDER_ID     = 1L;
    static final Long STAFF_ID     = 99L;

    // ── Factories ────────────────────────────────────────────────────────────

    static Order deliveredPickpointOrder() {
        return new Order.Builder()
                .id(ORDER_ID).userId(20L)
                .status(OrderStatus.DELIVERED)
                .deliveryType(DeliveryType.PICKPOINT)
                .pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(200)))
                .createdAt(Instant.now())
                .build();
    }

    static User staffUser(Long pickpointId) {
        return new User(STAFF_ID, new PhoneNumber("+992000000003"), UserRole.PICKPOINT_STAFF,
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

    static LookUpOrderForReturnService service(Order order, User staff) {
        return new LookUpOrderForReturnService(orderPort(order), userPort(staff));
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELIVERED PICKPOINT order at matching pickpoint → returns the order")
    void validLookup_returnsOrder() {
        var order  = deliveredPickpointOrder();
        var result = service(order, staffUser(PICKPOINT_ID)).execute(ORDER_ID, STAFF_ID);
        assertEquals(ORDER_ID, result.id());
    }

    @Test
    @DisplayName("Order not DELIVERED → OrderNotDeliveredException")
    void notDelivered_throws() {
        var shippedOrder = new Order.Builder()
                .id(ORDER_ID).userId(20L).status(OrderStatus.SHIPPED)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .build();

        assertThrows(OrderNotDeliveredException.class,
                () -> service(shippedOrder, staffUser(PICKPOINT_ID)).execute(ORDER_ID, STAFF_ID));
    }

    @Test
    @DisplayName("Staff at a different pickpoint → OrderNotAtPickpointException")
    void differentPickpoint_throws() {
        assertThrows(OrderNotAtPickpointException.class,
                () -> service(deliveredPickpointOrder(), staffUser(99L)).execute(ORDER_ID, STAFF_ID));
    }

    @Test
    @DisplayName("Non-PICKPOINT delivery type → OrderNotAtPickpointException")
    void nonPickpointDelivery_throws() {
        var homeOrder = new Order.Builder()
                .id(ORDER_ID).userId(20L).status(OrderStatus.DELIVERED)
                .deliveryType(DeliveryType.HOME).pickpointId(null)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .build();

        assertThrows(OrderNotAtPickpointException.class,
                () -> service(homeOrder, staffUser(PICKPOINT_ID)).execute(ORDER_ID, STAFF_ID));
    }

    @Test
    @DisplayName("Order not found → ResourceNotFoundException")
    void orderNotFound_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(null, staffUser(PICKPOINT_ID)).execute(999L, STAFF_ID));
    }
}
