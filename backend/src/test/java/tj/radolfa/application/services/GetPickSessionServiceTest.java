package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.readmodel.PickSession;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
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

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session assembled correctly: barcode and sizeLabel from SKU, counts from OrderItem")
    void sessionAssembly_barcodeAndSizeLabelFromSku_countsFromOrderItem() {
        OrderItem itemA = item(ITEM_A_ID, SKU_A_ID, 2, 1);
        OrderItem itemB = item(ITEM_B_ID, SKU_B_ID, 3, 0);
        Order order = order(ORDER_ID, OrderStatus.PAID, List.of(itemA, itemB));

        Sku skuA = sku(SKU_A_ID, "4000000000001", "XL");
        Sku skuB = sku(SKU_B_ID, "4000000000002", "S");

        var svc = new GetPickSessionService(
                new FakeLoadOrderPort(order),
                new FakeLoadSkuPort(Map.of(SKU_A_ID, skuA, SKU_B_ID, skuB)));

        PickSession session = svc.execute(ORDER_ID);

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
        var svc = new GetPickSessionService(
                new FakeLoadOrderPort(),
                new FakeLoadSkuPort(Map.of()));

        assertThrows(ResourceNotFoundException.class, () -> svc.execute(999L));
    }
}
