package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.MyOrdersSummary;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetMyOrdersSummaryServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 99L;

    /** One order per OrderStatus for USER_ID, plus a couple of orders for another user. */
    static List<Order> fixture() {
        List<Order> orders = List.of(OrderStatus.values()).stream()
                .map(status -> order((long) status.ordinal(), USER_ID, status))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        orders.add(order(100L, OTHER_USER_ID, OrderStatus.PAID));
        orders.add(order(101L, OTHER_USER_ID, OrderStatus.DELIVERED));
        return orders;
    }

    static Order order(Long id, Long userId, OrderStatus status) {
        return new Order.Builder()
                .id(id).userId(userId).status(status)
                .totalAmount(new Money(BigDecimal.valueOf(100))).createdAt(Instant.now())
                .deliveryType(DeliveryType.HOME)
                .build();
    }

    static LoadOrderPort fakePort(List<Order> orders) {
        return new LoadOrderPort() {
            @Override public List<Order> loadByUserId(Long userId) {
                return orders.stream().filter(o -> o.userId().equals(userId)).toList();
            }
            @Override public PageResult<Order> loadByUserIdPaged(Long userId, int page, int size) {
                return new PageResult<>(List.of(), 0, page, size, true);
            }
            @Override public long countByUserId(Long userId) {
                return orders.stream().filter(o -> o.userId().equals(userId)).count();
            }
            @Override public long countByUserIdAndStatuses(Long userId, Collection<OrderStatus> statuses) {
                return orders.stream()
                        .filter(o -> o.userId().equals(userId))
                        .filter(o -> statuses.contains(o.status()))
                        .count();
            }
            @Override public Optional<Order> loadById(Long id) { return Optional.empty(); }
            @Override public Optional<Order> loadByExternalOrderId(String externalOrderId) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long userId, int limit) { return List.of(); }
        };
    }

    @Test
    @DisplayName("Counts match the whitelist groups for the requesting user only")
    void countsMatchWhitelistGroups() {
        var svc = new GetMyOrdersSummaryService(fakePort(fixture()));

        MyOrdersSummary summary = svc.execute(USER_ID);

        assertEquals(OrderStatus.values().length, summary.all());
        assertEquals(9, summary.progress());  // PENDING, PAID, PICKED, CLAIMED, SHIPPED, OUT_FOR_DELIVERY, DELIVERY_ATTEMPTED, READY_FOR_PICKUP, RECALL_REQUESTED
        assertEquals(1, summary.delivered()); // DELIVERED
        assertEquals(3, summary.returns());   // RETURN_INITIATED, RETURNED_TO_WAREHOUSE, REFUNDED
    }

    @Test
    @DisplayName("Other users' orders are excluded")
    void excludesOtherUsersOrders() {
        var svc = new GetMyOrdersSummaryService(fakePort(fixture()));

        MyOrdersSummary summary = svc.execute(OTHER_USER_ID);

        assertEquals(2, summary.all());
        assertEquals(1, summary.progress());  // PAID
        assertEquals(1, summary.delivered()); // DELIVERED
        assertEquals(0, summary.returns());
    }
}
