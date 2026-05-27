package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.application.readmodel.PickSession;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.Warehouse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetPickSessionServiceTest {

    static final Long ORDER_ID  = 1L;
    static final Long SKU_A_ID  = 10L;
    static final Long SKU_B_ID  = 11L;
    static final Long ITEM_A_ID = 100L;
    static final Long ITEM_B_ID = 101L;
    static final Long WH_ID     = 1L;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    static OrderItem item(Long id, Long skuId, int qty, int picked) {
        return new OrderItem(id, skuId, null, "SKU-" + id, "Product " + id, qty,
                new Money(BigDecimal.TEN), picked, null, null);
    }

    static Sku sku(Long id, String barcode, String sizeLabel) {
        return new Sku(id, 1L, "SKU-" + id, sizeLabel, 5, new Money(BigDecimal.TEN), barcode);
    }

    static Order order(Long orderId, OrderStatus status, List<OrderItem> items) {
        return new Order.Builder()
                .id(orderId).userId(1L).externalOrderId("ORD-" + orderId)
                .status(status).deliveryType(DeliveryType.HOME)
                .items(items).createdAt(Instant.now())
                .build();
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadOrderPort implements LoadOrderPort {
        final Map<Long, Order> store;
        FakeLoadOrderPort(Order... orders) {
            store = new java.util.HashMap<>();
            for (Order o : orders) store.put(o.id(), o);
        }
        @Override public Optional<Order> loadById(Long id)                 { return Optional.ofNullable(store.get(id)); }
        @Override public List<Order>     loadByUserId(Long userId)         { return List.of(); }
        @Override public Optional<Order> loadByExternalOrderId(String id)  { return Optional.empty(); }
        @Override public List<Order>     loadRecentPaidByUserId(Long u, int l) { return List.of(); }
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

    static class FakeLoadWarehousePort implements LoadWarehousePort {
        @Override public Warehouse findDefault() {
            return new Warehouse(WH_ID, "MAIN", "Main Warehouse", true, Instant.now());
        }
        @Override public Optional<Warehouse> findById(Long id) {
            return id.equals(WH_ID) ? Optional.of(findDefault()) : Optional.empty();
        }
    }

    static class FakePlacementPort implements InventoryPlacementPort {
        final Map<Long, List<PlacementView>> views;
        FakePlacementPort(Map<Long, List<PlacementView>> views) { this.views = views; }

        @Override public List<PlacementView> placementViewsForSku(Long s, Long w)        { return views.getOrDefault(s, List.of()); }
        @Override public Map<Long, List<PlacementView>> placementViewsForSkus(Collection<Long> ids, Long w) { return views; }
        @Override public void addToInbound(Long s, Long w, int q)               {}
        @Override public boolean decrementForSale(Long s, Long w, int q)        { return true; }
        @Override public void putaway(Long s, Long w, Long b, int q)            {}
        @Override public void relocate(Long s, Long w, Long f, Long t, int q)   {}
        @Override public void adjustInbound(Long s, Long w, int d)              {}
        @Override public int totalForSku(Long s, Long w)                        { return 0; }
        @Override public List<InventoryPlacement> placementsForSku(Long s, Long w) { return List.of(); }
        @Override public PageResult<InboundQueueItem> findInboundQueue(int p, int sz, String q) {
            return new PageResult<>(List.of(), 0, p, sz, true);
        }
        @Override public boolean hasPlacementsInBin(Long binId) { return false; }
    }

    static final FakePlacementPort NO_PLACEMENTS = new FakePlacementPort(Map.of());

    static GetPickSessionService service(FakeLoadOrderPort orderPort, FakeLoadSkuPort skuPort) {
        return new GetPickSessionService(orderPort, skuPort, NO_PLACEMENTS, new FakeLoadWarehousePort());
    }

    static GetPickSessionService service(FakeLoadOrderPort orderPort, FakeLoadSkuPort skuPort,
                                          FakePlacementPort placementPort) {
        return new GetPickSessionService(orderPort, skuPort, placementPort, new FakeLoadWarehousePort());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session assembled correctly: barcode and sizeLabel from SKU, counts from OrderItem")
    void sessionAssembly_barcodeAndSizeLabelFromSku_countsFromOrderItem() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 2, 1);
        OrderItem itemB = item(ITEM_B_ID, SKU_B_ID, 3, 0);
        Order order = order(ORDER_ID, OrderStatus.PAID, List.of(itemA, itemB));

        Sku skuA = sku(SKU_A_ID, "4000000000001", "XL");
        Sku skuB = sku(SKU_B_ID, "4000000000002", "S");

        PickSession session = service(
                new FakeLoadOrderPort(order),
                new FakeLoadSkuPort(Map.of(SKU_A_ID, skuA, SKU_B_ID, skuB))).execute(ORDER_ID);

        assertEquals(ORDER_ID, session.orderId());
        assertEquals(OrderStatus.PAID, session.status());
        assertEquals(DeliveryType.HOME, session.deliveryType());
        assertEquals(2, session.items().size());

        PickSession.Item rowA = session.items().stream()
                .filter(i -> ITEM_A_ID.equals(i.orderItemId())).findFirst().orElseThrow();
        assertEquals("4000000000001", rowA.barcode());
        assertEquals("XL", rowA.sizeLabel());
        assertEquals(2, rowA.quantity());
        assertEquals(1, rowA.quantityPicked());

        PickSession.Item rowB = session.items().stream()
                .filter(i -> ITEM_B_ID.equals(i.orderItemId())).findFirst().orElseThrow();
        assertEquals("4000000000002", rowB.barcode());
        assertEquals("S", rowB.sizeLabel());
        assertEquals(3, rowB.quantity());
        assertEquals(0, rowB.quantityPicked());
    }

    @Test
    @DisplayName("Order missing → ResourceNotFoundException")
    void orderMissing_throwsResourceNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(new FakeLoadOrderPort(), new FakeLoadSkuPort(Map.of())).execute(999L));
    }

    @Test
    @DisplayName("Placements from port are attached to matching items, sorted bins-first")
    void placements_attachedToItems() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 2, 0);
        Order order = order(ORDER_ID, OrderStatus.PAID, List.of(itemA));
        Sku skuA = sku(SKU_A_ID, "4000000000001", "XL");

        var views = List.of(
                new PlacementView(1L, "A-1-1", 40),
                new PlacementView(2L, "B-2-3", 10),
                new PlacementView(null, null, 5));
        var placement = new FakePlacementPort(Map.of(SKU_A_ID, views));

        PickSession session = service(
                new FakeLoadOrderPort(order),
                new FakeLoadSkuPort(Map.of(SKU_A_ID, skuA)),
                placement).execute(ORDER_ID);

        PickSession.Item rowA = session.items().get(0);
        assertEquals(3, rowA.placements().size());
        assertEquals("A-1-1", rowA.placements().get(0).binLabel());
        assertEquals(40, rowA.placements().get(0).quantity());
        assertEquals("B-2-3", rowA.placements().get(1).binLabel());
        assertNull(rowA.placements().get(2).binLabel(), "inbound entry has null binLabel");
        assertEquals(5, rowA.placements().get(2).quantity());
    }

    @Test
    @DisplayName("Item with no placements gets an empty list (not null)")
    void itemWithNoPlacements_getsEmptyList() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 1, 0);
        Order order = order(ORDER_ID, OrderStatus.PAID, List.of(itemA));

        PickSession session = service(
                new FakeLoadOrderPort(order),
                new FakeLoadSkuPort(Map.of(SKU_A_ID, sku(SKU_A_ID, "BC", "M")))).execute(ORDER_ID);

        assertNotNull(session.items().get(0).placements());
        assertTrue(session.items().get(0).placements().isEmpty());
    }
}
