package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.MyOrderFilter;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GetMyOrdersServiceTest {

    private static final Long USER_ID = 10L;

    /** One order per OrderStatus, all owned by USER_ID. */
    static List<Order> ordersForAllStatuses() {
        return List.of(OrderStatus.values()).stream()
                .map(status -> new Order.Builder()
                        .id((long) status.ordinal())
                        .userId(USER_ID)
                        .status(status)
                        .totalAmount(new Money(BigDecimal.valueOf(100)))
                        .createdAt(Instant.now())
                        .deliveryType(DeliveryType.HOME)
                        .build())
                .toList();
    }

    /** In-memory fake that honors the status filter, mirroring the real adapter's contract. */
    static LoadOrderPort fakePort(List<Order> orders) {
        return new LoadOrderPort() {
            @Override public List<Order> loadByUserId(Long userId) {
                return orders.stream().filter(o -> o.userId().equals(userId)).toList();
            }
            @Override public PageResult<Order> loadByUserIdPaged(Long userId, int page, int size) {
                return loadByUserIdAndStatusesPaged(userId, List.of(), page, size);
            }
            @Override public PageResult<Order> loadByUserIdAndStatusesPaged(Long userId, Collection<OrderStatus> statuses,
                                                                              int page, int size) {
                List<Order> filtered = orders.stream()
                        .filter(o -> o.userId().equals(userId))
                        .filter(o -> statuses == null || statuses.isEmpty() || statuses.contains(o.status()))
                        .toList();
                return new PageResult<>(filtered, filtered.size(), page, size, true);
            }
            @Override public Optional<Order> loadById(Long id) { return Optional.empty(); }
            @Override public Optional<Order> loadByExternalOrderId(String externalOrderId) { return Optional.empty(); }
            @Override public List<Order> loadRecentPaidByUserId(Long userId, int limit) { return List.of(); }
        };
    }

    @Test
    @DisplayName("filter=all returns every status, including CANCELLED")
    void all_returnsEveryStatus() {
        var svc = new GetMyOrdersService(fakePort(ordersForAllStatuses()));

        PageResult<Order> result = svc.execute(USER_ID, MyOrderFilter.ALL, 1, 50);

        assertEquals(OrderStatus.values().length, result.content().size());
        assertTrue(result.content().stream().anyMatch(o -> o.status() == OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("filter=progress returns only the in-progress whitelist, including RECALL_REQUESTED")
    void progress_returnsOnlyWhitelistedStatuses() {
        var svc = new GetMyOrdersService(fakePort(ordersForAllStatuses()));

        PageResult<Order> result = svc.execute(USER_ID, MyOrderFilter.PROGRESS, 1, 50);

        Set<OrderStatus> expected = MyOrderFilter.PROGRESS.statuses();
        assertEquals(expected.size(), result.content().size());
        result.content().forEach(o -> assertTrue(expected.contains(o.status())));
        assertTrue(result.content().stream().anyMatch(o -> o.status() == OrderStatus.RECALL_REQUESTED));
    }

    @Test
    @DisplayName("filter=delivered returns only DELIVERED")
    void delivered_returnsOnlyDelivered() {
        var svc = new GetMyOrdersService(fakePort(ordersForAllStatuses()));

        PageResult<Order> result = svc.execute(USER_ID, MyOrderFilter.DELIVERED, 1, 50);

        assertEquals(1, result.content().size());
        assertEquals(OrderStatus.DELIVERED, result.content().get(0).status());
    }

    @Test
    @DisplayName("filter=returns returns only the returns whitelist")
    void returns_returnsOnlyReturnsWhitelist() {
        var svc = new GetMyOrdersService(fakePort(ordersForAllStatuses()));

        PageResult<Order> result = svc.execute(USER_ID, MyOrderFilter.RETURNS, 1, 50);

        Set<OrderStatus> expected = MyOrderFilter.RETURNS.statuses();
        assertEquals(expected.size(), result.content().size());
        result.content().forEach(o -> assertTrue(expected.contains(o.status())));
    }

    @Test
    @DisplayName("MyOrderFilter.fromParam: null or unknown values default to ALL")
    void fromParam_defaultsToAll() {
        assertEquals(MyOrderFilter.ALL, MyOrderFilter.fromParam(null));
        assertEquals(MyOrderFilter.ALL, MyOrderFilter.fromParam("garbage"));
        assertEquals(MyOrderFilter.ALL, MyOrderFilter.fromParam(""));
    }

    @Test
    @DisplayName("MyOrderFilter.fromParam: known values are case-insensitive")
    void fromParam_caseInsensitive() {
        assertEquals(MyOrderFilter.PROGRESS, MyOrderFilter.fromParam("progress"));
        assertEquals(MyOrderFilter.PROGRESS, MyOrderFilter.fromParam("PROGRESS"));
        assertEquals(MyOrderFilter.DELIVERED, MyOrderFilter.fromParam("Delivered"));
        assertEquals(MyOrderFilter.RETURNS, MyOrderFilter.fromParam("returns"));
        assertEquals(MyOrderFilter.ALL, MyOrderFilter.fromParam("all"));
    }
}
