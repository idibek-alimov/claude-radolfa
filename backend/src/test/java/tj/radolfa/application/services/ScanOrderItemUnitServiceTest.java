package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.ScanOrderItemUnitUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.ports.out.SaveOrderItemPickStatePort;
import tj.radolfa.domain.exception.BarcodeMismatchException;
import tj.radolfa.domain.exception.OrderItemAlreadyFullyPickedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.Warehouse;
import tj.radolfa.domain.model.WinningMechanism;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScanOrderItemUnitServiceTest {

    static final Long ACTOR_ID    = 99L;
    static final Long ORDER_ID    = 1L;
    static final Long SKU_A_ID    = 10L;
    static final Long SKU_B_ID    = 11L;
    static final Long ITEM_A_ID   = 100L;
    static final Long ITEM_B_ID   = 101L;
    static final String BARCODE_A = "4000000000001";
    static final String BARCODE_B = "4000000000002";

    // ── Domain fixtures ───────────────────────────────────────────────────────

    static OrderItem item(Long id, Long skuId, int qty, int picked) {
        return new OrderItem(id, skuId, null, "SKU-" + id, "Product " + id, qty,
                new Money(BigDecimal.TEN), picked, null, null, null, null, WinningMechanism.NONE, null, null);
    }

    static Sku sku(Long id, String barcode) {
        return new Sku(id, 1L, "SKU-" + id, "M", 10, new Money(BigDecimal.TEN), barcode);
    }

    static Order paidHomeOrder(Long orderId, List<OrderItem> items) {
        return new Order.Builder()
                .id(orderId).userId(1L).externalOrderId("ORD-" + orderId)
                .status(OrderStatus.PAID).deliveryType(DeliveryType.HOME)
                .items(items).createdAt(Instant.now())
                .build();
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadOrderPort implements LoadOrderPort {
        final Map<Long, Order> store = new LinkedHashMap<>();

        FakeLoadOrderPort(Order... orders) {
            for (Order o : orders) store.put(o.id(), o);
        }

        void update(Order order) { store.put(order.id(), order); }

        @Override public Optional<Order> loadById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Order>     loadByUserId(Long userId) { return List.of(); }
        @Override public Optional<Order> loadByExternalOrderId(String id) { return Optional.empty(); }
        @Override public List<Order>     loadRecentPaidByUserId(Long userId, int limit) { return List.of(); }
    }

    static class CapturingSaveOrderItemPickStatePort implements SaveOrderItemPickStatePort {
        record IncrementCall(Long orderItemId, Long actorUserId) {}
        final List<IncrementCall> calls = new ArrayList<>();
        final FakeLoadOrderPort orderPort;
        final Map<Long, Integer> maxQuantities;
        final Map<Long, Integer> currentCounts = new HashMap<>();

        CapturingSaveOrderItemPickStatePort(FakeLoadOrderPort orderPort, Map<Long, Integer> maxQuantities) {
            this.orderPort = orderPort;
            this.maxQuantities = maxQuantities;
        }

        @Override
        public int incrementPicked(Long orderItemId, Long actorUserId) {
            calls.add(new IncrementCall(orderItemId, actorUserId));
            int max     = maxQuantities.getOrDefault(orderItemId, 0);
            int current = currentCounts.getOrDefault(orderItemId, 0);
            if (current >= max) throw new OrderItemAlreadyFullyPickedException(orderItemId, max);
            int newCount = current + 1;
            currentCounts.put(orderItemId, newCount);
            // Reflect updated pick count back into the fake order port
            for (Map.Entry<Long, Order> entry : orderPort.store.entrySet()) {
                Order order = entry.getValue();
                if (order.items().stream().noneMatch(i -> orderItemId.equals(i.getId()))) continue;
                List<OrderItem> updated = order.items().stream()
                        .map(i -> {
                            if (!orderItemId.equals(i.getId())) return i;
                            return new OrderItem(i.getId(), i.getSkuId(), i.getListingVariantId(),
                                    i.getSkuCode(), i.getProductName(), i.getQuantity(), i.getPrice(),
                                    newCount, newCount >= i.getQuantity() ? Instant.now() : null, null, i.getSellerId(),
                                    i.getOriginalUnitPrice(), i.getMechanism(), i.getEffectiveDiscountPercent(), i.getLoyaltyTierPercent());
                        })
                        .toList();
                orderPort.update(order.toBuilder().items(updated).build());
                break;
            }
            return newCount;
        }
    }

    static class FakeLoadSkuPort implements LoadSkuPort {
        final Map<Long, Sku> store;
        FakeLoadSkuPort(Map<Long, Sku> store) { this.store = store; }

        @Override public Optional<Sku> findSkuById(Long id)               { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Sku> findBySkuCode(String code)         { return Optional.empty(); }
        @Override public List<Sku>     findSkusByVariantId(Long id)       { return List.of(); }
        @Override public List<Sku>     findAllByIds(Collection<Long> ids) {
            return ids.stream().filter(store::containsKey).map(store::get).toList();
        }
    }

    static class CapturingRecordInventoryTransactionPort implements RecordInventoryTransactionPort {
        final List<InventoryTransaction> records = new ArrayList<>();
        @Override public void record(InventoryTransaction tx) { records.add(tx); }
    }

    static class FakeLoadWarehousePort implements LoadWarehousePort {
        @Override public Warehouse findDefault() { return new Warehouse(1L, "MAIN", "Main", true, Instant.now()); }
        @Override public Optional<Warehouse> findById(Long id) { return Optional.of(findDefault()); }
    }

    ScanOrderItemUnitService service(FakeLoadOrderPort orderPort,
                                     FakeLoadSkuPort skuPort,
                                     CapturingSaveOrderItemPickStatePort savePort,
                                     CapturingRecordInventoryTransactionPort recordPort) {
        return new ScanOrderItemUnitService(orderPort, skuPort, savePort, recordPort,
                new FakeLoadWarehousePort());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1-item qty-1 order: one correct scan → orderFullyPicked=true, status unchanged, 1 ledger row (delta=0)")
    void singleItemSingleUnit_correctScan_fullyPicked() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 1, 0);
        Order order = paidHomeOrder(ORDER_ID, List.of(itemA));

        var orderPort  = new FakeLoadOrderPort(order);
        var savePort   = new CapturingSaveOrderItemPickStatePort(orderPort, Map.of(ITEM_A_ID, 1));
        var recordPort = new CapturingRecordInventoryTransactionPort();
        var skuPort    = new FakeLoadSkuPort(Map.of(SKU_A_ID, sku(SKU_A_ID, BARCODE_A)));

        var svc = service(orderPort, skuPort, savePort, recordPort);

        ScanOrderItemUnitUseCase.Result result =
                svc.execute(new ScanOrderItemUnitUseCase.Command(ORDER_ID, BARCODE_A, ACTOR_ID));

        assertTrue(result.orderFullyPicked());
        assertEquals(ITEM_A_ID, result.orderItemId());
        assertEquals(1, result.quantityPicked());
        assertEquals(1, result.quantityOrdered());

        assertEquals(1, recordPort.records.size());
        assertEquals(InventoryTransactionType.PICK_VERIFICATION, recordPort.records.get(0).type());
        assertEquals(0, recordPort.records.get(0).delta());
        assertEquals("ORDER_ITEM", recordPort.records.get(0).referenceType());
        assertEquals(ITEM_A_ID, recordPort.records.get(0).referenceId());
    }

    @Test
    @DisplayName("2-item qty-2 order: orderFullyPicked reported only on the 4th scan, status never changed")
    void twoItemsTwoUnits_fullyPickedOnFinalScan_noAutoComplete() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 2, 0);
        OrderItem itemB = item(ITEM_B_ID, SKU_B_ID, 2, 0);
        Order order = paidHomeOrder(ORDER_ID, List.of(itemA, itemB));

        var orderPort = new FakeLoadOrderPort(order);
        var savePort  = new CapturingSaveOrderItemPickStatePort(orderPort,
                Map.of(ITEM_A_ID, 2, ITEM_B_ID, 2));
        var recordPort = new CapturingRecordInventoryTransactionPort();
        var skuPort = new FakeLoadSkuPort(Map.of(
                SKU_A_ID, sku(SKU_A_ID, BARCODE_A),
                SKU_B_ID, sku(SKU_B_ID, BARCODE_B)));

        var svc = service(orderPort, skuPort, savePort, recordPort);
        var cmd = (java.util.function.Function<String, ScanOrderItemUnitUseCase.Result>)
                barcode -> svc.execute(new ScanOrderItemUnitUseCase.Command(ORDER_ID, barcode, ACTOR_ID));

        // scans 1–3: not fully picked yet
        assertFalse(cmd.apply(BARCODE_A).orderFullyPicked()); // itemA: 1/2
        assertFalse(cmd.apply(BARCODE_A).orderFullyPicked()); // itemA: 2/2, itemB: 0/2
        assertFalse(cmd.apply(BARCODE_B).orderFullyPicked()); // itemA: 2/2, itemB: 1/2

        // scan 4: all items picked — result signals fully-picked but order stays PAID
        assertTrue(cmd.apply(BARCODE_B).orderFullyPicked()); // itemA: 2/2, itemB: 2/2
        assertEquals(4, recordPort.records.size());
    }

    @Test
    @DisplayName("Wrong barcode → BarcodeMismatchException, no port writes")
    void wrongBarcode_throwsMismatch_noPortWrites() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 1, 0);
        Order order = paidHomeOrder(ORDER_ID, List.of(itemA));

        var orderPort  = new FakeLoadOrderPort(order);
        var savePort   = new CapturingSaveOrderItemPickStatePort(orderPort, Map.of(ITEM_A_ID, 1));
        var recordPort = new CapturingRecordInventoryTransactionPort();
        var skuPort    = new FakeLoadSkuPort(Map.of(SKU_A_ID, sku(SKU_A_ID, BARCODE_A)));

        var svc = service(orderPort, skuPort, savePort, recordPort);

        assertThrows(BarcodeMismatchException.class,
                () -> svc.execute(new ScanOrderItemUnitUseCase.Command(ORDER_ID, "0000000000000", ACTOR_ID)));

        assertTrue(savePort.calls.isEmpty());
        assertTrue(recordPort.records.isEmpty());
    }

    @Test
    @DisplayName("Scanning already fully-picked item → OrderItemAlreadyFullyPickedException, no port writes")
    void fullyPickedItem_throwsAlreadyFullyPicked_noPortWrites() {
        OrderItem fullyPicked = item(ITEM_A_ID, SKU_A_ID, 1, 1); // qty=1, picked=1
        Order order = paidHomeOrder(ORDER_ID, List.of(fullyPicked));

        var orderPort  = new FakeLoadOrderPort(order);
        var savePort   = new CapturingSaveOrderItemPickStatePort(orderPort, Map.of(ITEM_A_ID, 1));
        var recordPort = new CapturingRecordInventoryTransactionPort();
        var skuPort    = new FakeLoadSkuPort(Map.of(SKU_A_ID, sku(SKU_A_ID, BARCODE_A)));

        var svc = service(orderPort, skuPort, savePort, recordPort);

        assertThrows(OrderItemAlreadyFullyPickedException.class,
                () -> svc.execute(new ScanOrderItemUnitUseCase.Command(ORDER_ID, BARCODE_A, ACTOR_ID)));

        assertTrue(savePort.calls.isEmpty());
        assertTrue(recordPort.records.isEmpty());
    }

    @Test
    @DisplayName("Order not in PAID status → IllegalArgumentException")
    void orderNotPaid_throwsIllegalArgument() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 1, 0);
        Order shippedOrder = new Order.Builder()
                .id(ORDER_ID).userId(1L).externalOrderId("ORD-1")
                .status(OrderStatus.SHIPPED).deliveryType(DeliveryType.HOME)
                .items(List.of(itemA)).createdAt(Instant.now())
                .build();

        var orderPort  = new FakeLoadOrderPort(shippedOrder);
        var savePort   = new CapturingSaveOrderItemPickStatePort(orderPort, Map.of(ITEM_A_ID, 1));
        var recordPort = new CapturingRecordInventoryTransactionPort();
        var skuPort    = new FakeLoadSkuPort(Map.of(SKU_A_ID, sku(SKU_A_ID, BARCODE_A)));

        var svc = service(orderPort, skuPort, savePort, recordPort);

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new ScanOrderItemUnitUseCase.Command(ORDER_ID, BARCODE_A, ACTOR_ID)));
    }

    @Test
    @DisplayName("Order not found → ResourceNotFoundException")
    void orderNotFound_throwsResourceNotFound() {
        var orderPort  = new FakeLoadOrderPort(); // empty
        var savePort   = new CapturingSaveOrderItemPickStatePort(orderPort, Map.of());
        var recordPort = new CapturingRecordInventoryTransactionPort();
        var skuPort    = new FakeLoadSkuPort(Map.of());

        var svc = service(orderPort, skuPort, savePort, recordPort);

        assertThrows(ResourceNotFoundException.class,
                () -> svc.execute(new ScanOrderItemUnitUseCase.Command(999L, BARCODE_A, ACTOR_ID)));
    }
}
