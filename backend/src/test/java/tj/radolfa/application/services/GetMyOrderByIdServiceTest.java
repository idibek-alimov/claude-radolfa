package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetMyOrderByIdServiceTest {

    static Order order(Long id, Long userId) {
        return new Order.Builder()
                .id(id).userId(userId).status(OrderStatus.PAID)
                .totalAmount(new Money(BigDecimal.valueOf(200))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME)
                .build();
    }

    static LoadOrderPort portWith(Order order) {
        return new LoadOrderPort() {
            @Override public Optional<Order> loadById(Long id) {
                return order != null && order.id().equals(id) ? Optional.of(order) : Optional.empty();
            }
            @Override public List<Order> loadByUserId(Long u) { return List.of(); }
            @Override public PageResult<Order> loadByUserIdPaged(Long u, int p, int s) {
                return new PageResult<>(List.of(), 0, p, s, true);
            }
            @Override public Optional<Order> loadByExternalOrderId(String e) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long u, int l) { return List.of(); }
        };
    }

    @Test
    @DisplayName("Order belongs to requesting user → order returned")
    void ownOrder_returned() {
        var order = order(1L, 10L);
        var svc   = new GetMyOrderByIdService(portWith(order));

        Order result = svc.execute(1L, 10L);

        assertEquals(1L, result.id());
        assertEquals(10L, result.userId());
    }

    @Test
    @DisplayName("Order belongs to a different user → ResourceNotFoundException (404-safe)")
    void differentUser_throwsResourceNotFound() {
        var order = order(1L, 10L);
        var svc   = new GetMyOrderByIdService(portWith(order));

        var ex = assertThrows(ResourceNotFoundException.class, () -> svc.execute(1L, 99L));
        assertFalse(ex.getMessage().toLowerCase().contains("access"), "Must not say 'access denied'");
    }

    @Test
    @DisplayName("Order does not exist → ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFound() {
        var svc = new GetMyOrderByIdService(portWith(null));

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L, 10L));
    }

    @Test
    @DisplayName("Both wrong user and non-existent → same exception type, no differentiation")
    void sameExceptionType_forBothCases() {
        var order = order(1L, 10L);
        var svc   = new GetMyOrderByIdService(portWith(order));

        // Wrong user
        var ex1 = assertThrows(ResourceNotFoundException.class, () -> svc.execute(1L, 99L));
        // Not found
        var ex2 = assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L, 10L));

        // Both say "Order not found" — indistinguishable to a client
        assertTrue(ex1.getMessage().contains("Order not found"));
        assertTrue(ex2.getMessage().contains("Order not found"));
    }
}
