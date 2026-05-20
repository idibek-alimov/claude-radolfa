package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveOrderPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.*;
import tj.radolfa.domain.model.InventoryTransactionType;

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

    static final StockAdjustmentPort NO_STOCK = new StockAdjustmentPort() {
        @Override public void decrement(Long skuId, int qty) {}
        @Override public void increment(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}
    };

    static class CapturingStockAdjustmentPort implements StockAdjustmentPort {
        record IncrementCall(Long skuId, int qty, InventoryTransactionType type) {}
        final List<IncrementCall> calls = new ArrayList<>();

        @Override public void decrement(Long skuId, int qty) {}
        @Override public void increment(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}

        @Override
        public void increment(Long skuId, int qty, InventoryTransactionType type,
                              String refType, Long refId, Long actorUserId) {
            calls.add(new IncrementCall(skuId, qty, type));
        }
    }

    static ConfirmReturnedToWarehouseService service(Order order, User staff,
                                                      CapturingSaveOrderPort saveOrder) {
        return service(order, staff, saveOrder, NO_STOCK);
    }

    static ConfirmReturnedToWarehouseService service(Order order, User staff,
                                                      CapturingSaveOrderPort saveOrder,
                                                      StockAdjustmentPort stockPort) {
        return new ConfirmReturnedToWarehouseService(orderPort(order), saveOrder,
                userPort(staff), stockPort);
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

    @Test
    @DisplayName("Order with 2 items → stock incremented twice with RETURN_RESTORE type")
    void orderWithItems_stockRestoredForEach() {
        var item1 = new OrderItem(1L, 101L, null, "SKU-A", "Widget", 2,
                new Money(BigDecimal.TEN));
        var item2 = new OrderItem(2L, 102L, null, "SKU-B", "Gadget", 1,
                new Money(BigDecimal.TEN));
        Order orderWithItems = new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.RETURN_INITIATED)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(150))).createdAt(Instant.now())
                .items(List.of(item1, item2))
                .build();

        var capStock = new CapturingStockAdjustmentPort();
        service(orderWithItems, staffUser(PICKPOINT_ID), new CapturingSaveOrderPort(), capStock)
                .execute(ORDER_ID, 99L);

        assertEquals(2, capStock.calls.size());
        assertTrue(capStock.calls.stream().allMatch(c -> c.type() == InventoryTransactionType.RETURN_RESTORE));
        assertTrue(capStock.calls.stream().anyMatch(c -> c.skuId().equals(101L) && c.qty() == 2));
        assertTrue(capStock.calls.stream().anyMatch(c -> c.skuId().equals(102L) && c.qty() == 1));
    }

    @Test
    @DisplayName("Item with null skuId → stock increment skipped for that item")
    void itemWithNullSkuId_incrementSkipped() {
        var itemWithSku    = new OrderItem(1L, 101L, null, "SKU-A", "Widget", 3,
                new Money(BigDecimal.TEN));
        var itemWithoutSku = new OrderItem(2L, null, null, "SKU-DEL", "Deleted", 1,
                new Money(BigDecimal.TEN));
        Order orderWithItems = new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.RETURN_INITIATED)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(PICKPOINT_ID)
                .totalAmount(new Money(BigDecimal.valueOf(150))).createdAt(Instant.now())
                .items(List.of(itemWithSku, itemWithoutSku))
                .build();

        var capStock = new CapturingStockAdjustmentPort();
        service(orderWithItems, staffUser(PICKPOINT_ID), new CapturingSaveOrderPort(), capStock)
                .execute(ORDER_ID, 99L);

        assertEquals(1, capStock.calls.size());
        assertEquals(101L, capStock.calls.get(0).skuId());
    }
}
