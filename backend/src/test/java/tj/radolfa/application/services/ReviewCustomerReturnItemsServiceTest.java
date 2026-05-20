package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.ReviewCustomerReturnItemsUseCase;
import tj.radolfa.application.ports.in.warehouse.ReviewCustomerReturnItemsUseCase.Command;
import tj.radolfa.application.ports.in.warehouse.ReviewCustomerReturnItemsUseCase.ItemReview;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnItem;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.DeliveryType;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Resellability;
import tj.radolfa.domain.model.ReturnReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCustomerReturnItemsServiceTest {

    static final Long RETURN_ID    = 1L;
    static final Long ORDER_ID     = 10L;
    static final Long ADMIN_ID     = 99L;
    static final Long ORDER_ITEM_A = 100L;
    static final Long ORDER_ITEM_B = 101L;
    static final Long SKU_A        = 200L;
    static final Long SKU_B        = 201L;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    static CustomerReturnItem returnItem(Long id, Long orderItemId) {
        return new CustomerReturnItem(id, RETURN_ID, orderItemId, 2,
                ReturnReason.DAMAGED, null, Resellability.PENDING_REVIEW);
    }

    static CustomerReturn sentReturn(List<CustomerReturnItem> items) {
        return new CustomerReturn(RETURN_ID, ORDER_ID, 5L, 55L, Instant.now(), null,
                items, CustomerReturnStatus.SENT_TO_WAREHOUSE, Instant.now(), 55L,
                null, null, null, null);
    }

    static Order orderWithItems() {
        var itemA = new OrderItem(ORDER_ITEM_A, SKU_A, null, "SKU-A", "Widget", 2, new Money(BigDecimal.TEN));
        var itemB = new OrderItem(ORDER_ITEM_B, SKU_B, null, "SKU-B", "Gadget", 2, new Money(BigDecimal.TEN));
        return new Order.Builder()
                .id(ORDER_ID).userId(10L).status(OrderStatus.RETURNED_TO_WAREHOUSE)
                .deliveryType(DeliveryType.PICKPOINT).pickpointId(5L)
                .totalAmount(new Money(BigDecimal.valueOf(40))).createdAt(Instant.now())
                .items(List.of(itemA, itemB))
                .build();
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeLoadCustomerReturnPort implements LoadCustomerReturnPort {
        final CustomerReturn stored;
        FakeLoadCustomerReturnPort(CustomerReturn stored) { this.stored = stored; }

        @Override public Optional<CustomerReturn> loadById(Long id) {
            return stored != null && stored.getId().equals(id) ? Optional.of(stored) : Optional.empty();
        }
        @Override public List<CustomerReturn>    loadAllByOrderId(Long oid) { return List.of(); }
        @Override public PageResult<CustomerReturn> loadByPickpointIdAndStatus(
                Long p, tj.radolfa.domain.model.CustomerReturnStatus s, int pg, int sz) {
            return new PageResult<>(List.of(), 0, pg, sz, true);
        }
        @Override public PageResult<CustomerReturn> loadAllPaged(int pg, int sz, String search) {
            return new PageResult<>(List.of(), 0, pg, sz, true);
        }
    }

    static class CapturingSaveCustomerReturnPort implements SaveCustomerReturnPort {
        CustomerReturn saved;
        @Override public CustomerReturn save(CustomerReturn r) { saved = r; return r; }
    }

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

    static class CapturingRecordInventoryTransactionPort implements RecordInventoryTransactionPort {
        final List<InventoryTransaction> recorded = new ArrayList<>();
        @Override public void record(InventoryTransaction tx) { recorded.add(tx); }
    }

    static ReviewCustomerReturnItemsService service(
            CustomerReturn cr, Order order,
            CapturingSaveCustomerReturnPort save,
            CapturingStockAdjustmentPort stock,
            CapturingRecordInventoryTransactionPort ledger) {
        return new ReviewCustomerReturnItemsService(
                new FakeLoadCustomerReturnPort(cr),
                save,
                new LoadOrderPort() {
                    @Override public Optional<Order> loadById(Long id) {
                        return order != null && order.id().equals(id) ? Optional.of(order) : Optional.empty();
                    }
                    @Override public List<Order> loadByUserId(Long u) { return List.of(); }
                    @Override public Optional<Order> loadByExternalOrderId(String e) { return Optional.empty(); }
                    @Override public List<Order> loadRecentPaidByUserId(Long u, int l) { return List.of(); }
                },
                stock,
                ledger);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2 items both RESELLABLE → 2 RETURN_RESTORE increments; saved items updated")
    void bothResellable_incrementTwice() {
        var items = List.of(returnItem(1L, ORDER_ITEM_A), returnItem(2L, ORDER_ITEM_B));
        var save  = new CapturingSaveCustomerReturnPort();
        var stock = new CapturingStockAdjustmentPort();
        var ledger = new CapturingRecordInventoryTransactionPort();

        service(sentReturn(items), orderWithItems(), save, stock, ledger).execute(new Command(
                RETURN_ID, ADMIN_ID,
                List.of(new ItemReview(ORDER_ITEM_A, Resellability.RESELLABLE),
                        new ItemReview(ORDER_ITEM_B, Resellability.RESELLABLE))));

        assertEquals(2, stock.calls.size());
        stock.calls.forEach(c -> assertEquals(InventoryTransactionType.RETURN_RESTORE, c.type()));
        assertTrue(ledger.recorded.isEmpty());
        save.saved.getItems().forEach(i -> assertEquals(Resellability.RESELLABLE, i.resellability()));
    }

    @Test
    @DisplayName("1 RESELLABLE + 1 DEFECTIVE → 1 increment + 1 WRITE_OFF record (delta=0)")
    void oneResellableOneDefective_correctActions() {
        var items = List.of(returnItem(1L, ORDER_ITEM_A), returnItem(2L, ORDER_ITEM_B));
        var save  = new CapturingSaveCustomerReturnPort();
        var stock = new CapturingStockAdjustmentPort();
        var ledger = new CapturingRecordInventoryTransactionPort();

        service(sentReturn(items), orderWithItems(), save, stock, ledger).execute(new Command(
                RETURN_ID, ADMIN_ID,
                List.of(new ItemReview(ORDER_ITEM_A, Resellability.RESELLABLE),
                        new ItemReview(ORDER_ITEM_B, Resellability.DEFECTIVE))));

        assertEquals(1, stock.calls.size());
        assertEquals(InventoryTransactionType.RETURN_RESTORE, stock.calls.get(0).type());
        assertEquals(1, ledger.recorded.size());
        assertEquals(InventoryTransactionType.WRITE_OFF, ledger.recorded.get(0).type());
        assertEquals(0, ledger.recorded.get(0).delta());
    }

    @Test
    @DisplayName("Return not in SENT_TO_WAREHOUSE → IllegalStateException, nothing saved")
    void wrongStatus_throwsIllegalState() {
        CustomerReturn receivedReturn = new CustomerReturn(RETURN_ID, ORDER_ID, 5L, 55L,
                Instant.now(), null, List.of(), CustomerReturnStatus.RECEIVED,
                null, null, null, null, null, null);
        var save = new CapturingSaveCustomerReturnPort();

        assertThrows(IllegalStateException.class,
                () -> service(receivedReturn, orderWithItems(), save,
                        new CapturingStockAdjustmentPort(), new CapturingRecordInventoryTransactionPort())
                        .execute(new Command(RETURN_ID, ADMIN_ID,
                                List.of(new ItemReview(ORDER_ITEM_A, Resellability.RESELLABLE)))));

        assertNull(save.saved);
    }

    @Test
    @DisplayName("All DEFECTIVE → zero stock increments; 2 WRITE_OFF ledger rows")
    void allDefective_noStockRestored() {
        var items = List.of(returnItem(1L, ORDER_ITEM_A), returnItem(2L, ORDER_ITEM_B));
        var stock  = new CapturingStockAdjustmentPort();
        var ledger = new CapturingRecordInventoryTransactionPort();

        service(sentReturn(items), orderWithItems(), new CapturingSaveCustomerReturnPort(),
                stock, ledger).execute(new Command(RETURN_ID, ADMIN_ID,
                List.of(new ItemReview(ORDER_ITEM_A, Resellability.DEFECTIVE),
                        new ItemReview(ORDER_ITEM_B, Resellability.DEFECTIVE))));

        assertTrue(stock.calls.isEmpty());
        assertEquals(2, ledger.recorded.size());
        ledger.recorded.forEach(tx -> {
            assertEquals(InventoryTransactionType.WRITE_OFF, tx.type());
            assertEquals(0, tx.delta());
        });
    }

    @Test
    @DisplayName("Item with no matching review in command → item keeps PENDING_REVIEW, no crash")
    void itemNotInCommand_keepsPendingReview() {
        var items = List.of(returnItem(1L, ORDER_ITEM_A), returnItem(2L, ORDER_ITEM_B));
        var save  = new CapturingSaveCustomerReturnPort();

        service(sentReturn(items), orderWithItems(), save,
                new CapturingStockAdjustmentPort(), new CapturingRecordInventoryTransactionPort())
                .execute(new Command(RETURN_ID, ADMIN_ID,
                        List.of(new ItemReview(ORDER_ITEM_A, Resellability.RESELLABLE))));
        // ORDER_ITEM_B not in command

        CustomerReturnItem itemB = save.saved.getItems().stream()
                .filter(i -> i.orderItemId().equals(ORDER_ITEM_B))
                .findFirst().orElseThrow();
        assertEquals(Resellability.PENDING_REVIEW, itemB.resellability());
    }
}
