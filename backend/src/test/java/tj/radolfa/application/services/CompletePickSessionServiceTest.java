package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.in.warehouse.CompletePickSessionUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompletePickSessionServiceTest {

    static final Long ACTOR_ID  = 99L;
    static final Long ORDER_ID  = 1L;
    static final Long SKU_A_ID  = 10L;
    static final Long ITEM_A_ID = 100L;
    static final Long ITEM_B_ID = 101L;

    // ── Domain fixtures ───────────────────────────────────────────────────────

    static OrderItem item(Long id, Long skuId, int qty, int picked) {
        return new OrderItem(id, skuId, null, "SKU-" + id, "Product " + id, qty,
                new Money(BigDecimal.TEN), picked, picked >= qty ? Instant.now() : null, null);
    }

    static Order orderWithStatus(OrderStatus status, List<OrderItem> items) {
        return new Order.Builder()
                .id(ORDER_ID).userId(1L).externalOrderId("ORD-" + ORDER_ID)
                .status(status).deliveryType(DeliveryType.HOME)
                .items(items).createdAt(Instant.now())
                .build();
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadOrderPort implements LoadOrderPort {
        final Map<Long, Order> store = new LinkedHashMap<>();

        FakeLoadOrderPort(Order... orders) {
            for (Order o : orders) store.put(o.id(), o);
        }

        @Override public Optional<Order> loadById(Long id)                         { return Optional.ofNullable(store.get(id)); }
        @Override public List<Order>     loadByUserId(Long userId)                 { return List.of(); }
        @Override public Optional<Order> loadByExternalOrderId(String externalOrderId) { return Optional.empty(); }
        @Override public List<Order>     loadRecentPaidByUserId(Long userId, int limit) { return List.of(); }
    }

    static class CapturingUpdateOrderStatusUseCase implements UpdateOrderStatusUseCase {
        final List<Command> commands = new ArrayList<>();
        @Override public void execute(Command cmd) { commands.add(cmd); }
    }

    CompletePickSessionService service(FakeLoadOrderPort orderPort,
                                       CapturingUpdateOrderStatusUseCase statusUseCase) {
        return new CompletePickSessionService(orderPort, statusUseCase);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PAID + all items fully picked → exactly one PICKED command captured")
    void allItemsPicked_paid_completesOrder() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 1, 1); // qty=1, picked=1
        Order order = orderWithStatus(OrderStatus.PAID, List.of(itemA));

        var orderPort     = new FakeLoadOrderPort(order);
        var statusUseCase = new CapturingUpdateOrderStatusUseCase();
        var svc           = service(orderPort, statusUseCase);

        svc.execute(new CompletePickSessionUseCase.Command(ORDER_ID, ACTOR_ID));

        assertEquals(1, statusUseCase.commands.size());
        assertEquals(OrderStatus.PICKED, statusUseCase.commands.get(0).newStatus());
        assertEquals(ORDER_ID, statusUseCase.commands.get(0).orderId());
    }

    @Test
    @DisplayName("PAID + multi-item all fully picked → exactly one PICKED command")
    void multipleItemsAllPicked_paid_completesOrder() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 2, 2);
        OrderItem itemB = item(ITEM_B_ID, 11L,      1, 1);
        Order order = orderWithStatus(OrderStatus.PAID, List.of(itemA, itemB));

        var orderPort     = new FakeLoadOrderPort(order);
        var statusUseCase = new CapturingUpdateOrderStatusUseCase();
        var svc           = service(orderPort, statusUseCase);

        svc.execute(new CompletePickSessionUseCase.Command(ORDER_ID, ACTOR_ID));

        assertEquals(1, statusUseCase.commands.size());
        assertEquals(OrderStatus.PICKED, statusUseCase.commands.get(0).newStatus());
    }

    @Test
    @DisplayName("Not all items picked → IllegalArgumentException, no command captured")
    void notFullyPicked_throwsIllegalArgument_noCommand() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 2, 1); // qty=2, picked=1 — not done
        Order order = orderWithStatus(OrderStatus.PAID, List.of(itemA));

        var orderPort     = new FakeLoadOrderPort(order);
        var statusUseCase = new CapturingUpdateOrderStatusUseCase();
        var svc           = service(orderPort, statusUseCase);

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new CompletePickSessionUseCase.Command(ORDER_ID, ACTOR_ID)));

        assertTrue(statusUseCase.commands.isEmpty());
    }

    @Test
    @DisplayName("Status not PAID (e.g. SHIPPED) → IllegalArgumentException, no command captured")
    void orderNotPaid_throwsIllegalArgument_noCommand() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 1, 1);
        Order order = orderWithStatus(OrderStatus.SHIPPED, List.of(itemA));

        var orderPort     = new FakeLoadOrderPort(order);
        var statusUseCase = new CapturingUpdateOrderStatusUseCase();
        var svc           = service(orderPort, statusUseCase);

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new CompletePickSessionUseCase.Command(ORDER_ID, ACTOR_ID)));

        assertTrue(statusUseCase.commands.isEmpty());
    }

    @Test
    @DisplayName("Order not found → ResourceNotFoundException, no command captured")
    void orderNotFound_throwsResourceNotFound_noCommand() {
        var orderPort     = new FakeLoadOrderPort(); // empty
        var statusUseCase = new CapturingUpdateOrderStatusUseCase();
        var svc           = service(orderPort, statusUseCase);

        assertThrows(ResourceNotFoundException.class,
                () -> svc.execute(new CompletePickSessionUseCase.Command(999L, ACTOR_ID)));

        assertTrue(statusUseCase.commands.isEmpty());
    }
}
