package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.domain.exception.InsufficientStockException;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.Warehouse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProductStockServiceTest {

    private static final Long SKU_ID     = 1L;
    private static final Long VARIANT_ID = 10L;
    private static final Long ORDER_ID   = 99L;
    private static final Long USER_ID    = 42L;
    private static final Long BIN_A      = 100L;
    private static final Long BIN_B      = 200L;

    private FakeInventoryPlacementPort       placementPort;
    private InMemoryLoadSkuPort              loadSkuPort;
    private FakeRecordInventoryTransactionPort ledgerPort;
    private UpdateProductStockService        service;

    static final LoadWarehousePort FAKE_WAREHOUSE = new LoadWarehousePort() {
        @Override public Warehouse findDefault() {
            return new Warehouse(1L, "MAIN", "Main Warehouse", true, Instant.now());
        }
        @Override public Optional<Warehouse> findById(Long id) {
            return id == 1L ? Optional.of(findDefault()) : Optional.empty();
        }
    };

    @BeforeEach
    void setUp() {
        placementPort = new FakeInventoryPlacementPort();
        loadSkuPort   = new InMemoryLoadSkuPort();
        ledgerPort    = new FakeRecordInventoryTransactionPort();
        service = new UpdateProductStockService(loadSkuPort, placementPort, ledgerPort, FAKE_WAREHOUSE);
    }

    // ── decrement (legacy signature) ──────────────────────────────────────────

    @Test
    void decrement_happyPath_stockReduced() {
        placementPort.put(SKU_ID, 5);

        service.decrement(SKU_ID, 3);

        assertEquals(2, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        assertEquals(InventoryTransactionType.SALE, ledgerPort.recorded.get(0).type());
        assertEquals(-3, ledgerPort.recorded.get(0).delta());
    }

    @Test
    void decrement_exactStock_stockBecomesZero() {
        placementPort.put(SKU_ID, 2);

        service.decrement(SKU_ID, 2);

        assertEquals(0, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
    }

    @Test
    void decrement_insufficientStock_throwsInsufficientStockException_andNoLedgerRow() {
        placementPort.put(SKU_ID, 1);

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> service.decrement(SKU_ID, 5)
        );

        assertEquals(SKU_ID, ex.getSkuId());
        assertEquals(1, ex.getAvailable());
        assertEquals(5, ex.getRequested());
        assertEquals(1, placementPort.stockOf(SKU_ID));
        assertTrue(ledgerPort.recorded.isEmpty(), "No ledger row on failed decrement");
    }

    @Test
    void decrement_zeroQuantity_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.decrement(SKU_ID, 0));
    }

    @Test
    void decrement_negativeQuantity_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.decrement(SKU_ID, -1));
    }

    // ── decrement (context-aware overload) ────────────────────────────────────

    @Test
    @DisplayName("decrement with orderId+actorUserId records SALE ledger row with refs")
    void decrement_withContext_recordsSaleLedgerRow() {
        placementPort.put(SKU_ID, 10);

        service.decrement(SKU_ID, 3, ORDER_ID, USER_ID);

        assertEquals(7, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        InventoryTransaction tx = ledgerPort.recorded.get(0);
        assertEquals(InventoryTransactionType.SALE, tx.type());
        assertEquals(-3, tx.delta());
        assertEquals(ORDER_ID, tx.referenceId());
        assertEquals("ORDER", tx.referenceType());
        assertEquals(USER_ID, tx.actorUserId());
    }

    @Test
    @DisplayName("decrement with context fails → no ledger row")
    void decrement_withContext_insufficientStock_noLedgerRow() {
        placementPort.put(SKU_ID, 1);

        assertThrows(InsufficientStockException.class,
                () -> service.decrement(SKU_ID, 5, ORDER_ID, USER_ID));

        assertTrue(ledgerPort.recorded.isEmpty());
    }

    // ── decrement: inbound-first then bins-desc spill ─────────────────────────

    @Test
    @DisplayName("decrement drains inbound first, then bins by quantity DESC")
    void decrement_spillsInboundFirstThenBinsDesc() {
        placementPort.put(SKU_ID, 10);          // inbound = 10
        placementPort.putBin(SKU_ID, BIN_A, 40); // bin A = 40
        placementPort.putBin(SKU_ID, BIN_B, 20); // bin B = 20

        service.decrement(SKU_ID, 55);  // drain: 10 inbound + 40 bin A + 5 from bin B

        assertEquals(0,  placementPort.inboundOf(SKU_ID));
        assertEquals(0,  placementPort.binOf(SKU_ID, BIN_A));
        assertEquals(15, placementPort.binOf(SKU_ID, BIN_B));
        assertEquals(15, placementPort.stockOf(SKU_ID));
        assertEquals(-55, ledgerPort.recorded.get(0).delta());
    }

    @Test
    @DisplayName("decrement beyond total → exception, placements unchanged")
    void decrement_beyondTotal_placementsUnchanged() {
        placementPort.put(SKU_ID, 5);
        placementPort.putBin(SKU_ID, BIN_A, 3);  // total = 8

        assertThrows(InsufficientStockException.class, () -> service.decrement(SKU_ID, 9));

        assertEquals(5, placementPort.inboundOf(SKU_ID));
        assertEquals(3, placementPort.binOf(SKU_ID, BIN_A));
        assertTrue(ledgerPort.recorded.isEmpty());
    }

    // ── increment (legacy signature) ──────────────────────────────────────────

    @Test
    void increment_happyPath_stockIncreased() {
        placementPort.put(SKU_ID, 3);
        loadSkuPort.put(sku(SKU_ID, 3));

        service.increment(SKU_ID, 7);

        assertEquals(10, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        assertEquals(InventoryTransactionType.CANCELLATION, ledgerPort.recorded.get(0).type());
        assertEquals(7, ledgerPort.recorded.get(0).delta());
    }

    @Test
    void increment_skuNotFound_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.increment(99L, 1));
    }

    @Test
    void increment_zeroQuantity_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.increment(SKU_ID, 0));
    }

    @Test
    @DisplayName("increment (RECEIPT) lands entirely in the inbound pool")
    void increment_receipt_landsInInbound() {
        loadSkuPort.put(sku(SKU_ID, 0));
        placementPort.putBin(SKU_ID, BIN_A, 10);  // existing bin placement untouched

        service.increment(SKU_ID, 30, InventoryTransactionType.RECEIPT, "STOCK_RECEIPT", 5L, USER_ID);

        assertEquals(30, placementPort.inboundOf(SKU_ID));
        assertEquals(10, placementPort.binOf(SKU_ID, BIN_A));
        assertEquals(40, placementPort.stockOf(SKU_ID));
        assertEquals(InventoryTransactionType.RECEIPT, ledgerPort.recorded.get(0).type());
        assertEquals(30, ledgerPort.recorded.get(0).delta());
    }

    // ── increment (context-aware overload) ────────────────────────────────────

    @Test
    @DisplayName("increment with CANCELLATION type records correct ledger row")
    void increment_withContext_recordsCorrectType() {
        placementPort.put(SKU_ID, 5);
        loadSkuPort.put(sku(SKU_ID, 5));

        service.increment(SKU_ID, 2, InventoryTransactionType.CANCELLATION, "ORDER", ORDER_ID, USER_ID);

        assertEquals(7, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        InventoryTransaction tx = ledgerPort.recorded.get(0);
        assertEquals(InventoryTransactionType.CANCELLATION, tx.type());
        assertEquals(2, tx.delta());
        assertEquals(ORDER_ID, tx.referenceId());
        assertEquals("ORDER", tx.referenceType());
        assertEquals(USER_ID, tx.actorUserId());
    }

    @Test
    @DisplayName("increment with RECALL_RETURN type records correct ledger row")
    void increment_recallReturn_recordsCorrectType() {
        placementPort.put(SKU_ID, 5);
        loadSkuPort.put(sku(SKU_ID, 5));

        service.increment(SKU_ID, 3, InventoryTransactionType.RECALL_RETURN, "ORDER", ORDER_ID, USER_ID);

        assertEquals(8, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        assertEquals(InventoryTransactionType.RECALL_RETURN, ledgerPort.recorded.get(0).type());
    }

    // ── setAbsolute ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setAbsolute raises stock → records MANUAL_ADJUSTMENT with positive delta")
    void setAbsolute_increaseStock_recordsManualAdjustment() {
        placementPort.put(SKU_ID, 7);

        service.setAbsolute(SKU_ID, 10, USER_ID);

        assertEquals(10, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        InventoryTransaction tx = ledgerPort.recorded.get(0);
        assertEquals(InventoryTransactionType.MANUAL_ADJUSTMENT, tx.type());
        assertEquals(3, tx.delta());
        assertEquals(USER_ID, tx.actorUserId());
        assertEquals("MANUAL", tx.referenceType());
        assertNull(tx.referenceId());
    }

    @Test
    @DisplayName("setAbsolute lowers stock → records MANUAL_ADJUSTMENT with negative delta")
    void setAbsolute_decreaseStock_recordsManualAdjustment() {
        placementPort.put(SKU_ID, 10);

        service.setAbsolute(SKU_ID, 3, USER_ID);

        assertEquals(3, placementPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        assertEquals(-7, ledgerPort.recorded.get(0).delta());
    }

    @Test
    @DisplayName("setAbsolute to same value → no ledger row recorded")
    void setAbsolute_noDelta_noLedgerRow() {
        placementPort.put(SKU_ID, 7);

        service.setAbsolute(SKU_ID, 7, USER_ID);

        assertTrue(ledgerPort.recorded.isEmpty(), "delta=0 must not write a ledger row");
    }

    // ── concurrency ───────────────────────────────────────────────────────────

    @Test
    void decrement_concurrent_neverOversells() throws InterruptedException {
        int initialStock  = 500;
        int threads       = 1000;
        placementPort.put(SKU_ID, initialStock);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures  = new AtomicInteger();
        CountDownLatch ready    = new CountDownLatch(threads);
        CountDownLatch start    = new CountDownLatch(1);
        ExecutorService pool    = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException ignored) {}
                try {
                    service.decrement(SKU_ID, 1);
                    successes.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failures.incrementAndGet();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        //noinspection ResultOfMethodCallIgnored
        pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(initialStock, successes.get(), "Exactly initialStock decrements should succeed");
        assertEquals(threads - initialStock, failures.get(), "Remaining calls must fail with InsufficientStockException");
        assertEquals(0, placementPort.stockOf(SKU_ID), "Stock must reach exactly 0");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Sku sku(Long id, int stock) {
        return new Sku(id, VARIANT_ID, "SKU-" + id, "M", stock, new Money(BigDecimal.TEN));
    }

    // ── in-memory fakes ───────────────────────────────────────────────────────

    static class FakeRecordInventoryTransactionPort implements RecordInventoryTransactionPort {
        final List<InventoryTransaction> recorded = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void record(InventoryTransaction transaction) {
            recorded.add(transaction);
        }
    }

    /**
     * In-memory implementation of InventoryPlacementPort.
     * Keyed by (skuId, binId) where binId=null means the inbound pool.
     * All mutating methods are synchronized to support the concurrency test.
     */
    static class FakeInventoryPlacementPort implements InventoryPlacementPort {

        // key: skuId * 10000 + (binId == null ? 0 : binId) — simple composite for test scale
        // Using a nested map for clarity: skuId → (binId/*null for inbound*/ → quantity)
        private final Map<Long, Map<Long, Integer>> store = new HashMap<>();

        void put(Long skuId, int qty) {
            store.computeIfAbsent(skuId, k -> new HashMap<>()).put(null, qty);
        }

        void putBin(Long skuId, Long binId, int qty) {
            store.computeIfAbsent(skuId, k -> new HashMap<>()).put(binId, qty);
        }

        int stockOf(Long skuId) {
            Map<Long, Integer> bins = store.get(skuId);
            if (bins == null) return 0;
            return bins.values().stream().mapToInt(Integer::intValue).sum();
        }

        int inboundOf(Long skuId) {
            Map<Long, Integer> bins = store.get(skuId);
            if (bins == null) return 0;
            return bins.getOrDefault(null, 0);
        }

        int binOf(Long skuId, Long binId) {
            Map<Long, Integer> bins = store.get(skuId);
            if (bins == null) return 0;
            return bins.getOrDefault(binId, 0);
        }

        @Override
        public synchronized void addToInbound(Long skuId, Long warehouseId, int qty) {
            Map<Long, Integer> bins = store.computeIfAbsent(skuId, k -> new HashMap<>());
            bins.merge(null, qty, Integer::sum);
        }

        @Override
        public synchronized boolean decrementForSale(Long skuId, Long warehouseId, int qty) {
            int total = stockOf(skuId);
            if (total < qty) return false;

            Map<Long, Integer> bins = store.get(skuId);
            int remaining = qty;

            // drain: inbound (null key) first, then bin keys by quantity DESC
            List<Map.Entry<Long, Integer>> ordered = new ArrayList<>(bins.entrySet());
            ordered.sort((a, b) -> {
                if (a.getKey() == null) return -1;
                if (b.getKey() == null) return 1;
                return Integer.compare(b.getValue(), a.getValue()); // DESC
            });

            for (Map.Entry<Long, Integer> entry : ordered) {
                if (remaining <= 0) break;
                int take = Math.min(entry.getValue(), remaining);
                if (take > 0) {
                    entry.setValue(entry.getValue() - take);
                    remaining -= take;
                }
            }
            return true;
        }

        @Override
        public synchronized void putaway(Long skuId, Long warehouseId, Long binId, int qty) {
            Map<Long, Integer> bins = store.computeIfAbsent(skuId, k -> new HashMap<>());
            int inbound = bins.getOrDefault(null, 0);
            bins.put(null, inbound - qty);
            bins.merge(binId, qty, Integer::sum);
        }

        @Override
        public synchronized void relocate(Long skuId, Long warehouseId, Long fromBinId, Long toBinId, int qty) {
            Map<Long, Integer> bins = store.computeIfAbsent(skuId, k -> new HashMap<>());
            bins.merge(fromBinId, -qty, Integer::sum);
            bins.merge(toBinId, qty, Integer::sum);
        }

        @Override
        public synchronized void adjustInbound(Long skuId, Long warehouseId, int delta) {
            Map<Long, Integer> bins = store.computeIfAbsent(skuId, k -> new HashMap<>());
            if (delta >= 0) {
                bins.merge(null, delta, Integer::sum);
            } else {
                // drain inbound-first then bins-desc (mirrors decrementForSale order)
                int toDrain = -delta;
                List<Map.Entry<Long, Integer>> ordered = new ArrayList<>(bins.entrySet());
                ordered.sort((a, b) -> {
                    if (a.getKey() == null) return -1;
                    if (b.getKey() == null) return 1;
                    return Integer.compare(b.getValue(), a.getValue());
                });
                for (Map.Entry<Long, Integer> entry : ordered) {
                    if (toDrain <= 0) break;
                    int take = Math.min(entry.getValue(), toDrain);
                    entry.setValue(entry.getValue() - take);
                    toDrain -= take;
                }
            }
        }

        @Override
        public synchronized int totalForSku(Long skuId, Long warehouseId) {
            return stockOf(skuId);
        }

        @Override
        public synchronized List<InventoryPlacement> placementsForSku(Long skuId, Long warehouseId) {
            Map<Long, Integer> bins = store.get(skuId);
            if (bins == null) return List.of();
            List<InventoryPlacement> result = new ArrayList<>();
            long idSeq = 1;
            for (Map.Entry<Long, Integer> e : bins.entrySet()) {
                result.add(new InventoryPlacement(idSeq++, skuId, warehouseId, e.getKey(), e.getValue()));
            }
            return result;
        }

        @Override
        public tj.radolfa.domain.model.PageResult<tj.radolfa.application.readmodel.InboundQueueItem>
                findInboundQueue(int page, int size, String search) {
            return new tj.radolfa.domain.model.PageResult<>(List.of(), 0, page, size, true);
        }

        @Override
        public boolean hasPlacementsInBin(Long binId) { return false; }

        @Override
        public List<tj.radolfa.domain.model.PlacementView> placementViewsForSku(Long skuId, Long warehouseId) {
            return List.of();
        }

        @Override
        public java.util.Map<Long, List<tj.radolfa.domain.model.PlacementView>> placementViewsForSkus(
                java.util.Collection<Long> skuIds, Long warehouseId) {
            return java.util.Map.of();
        }
    }

    static class InMemoryLoadSkuPort implements LoadSkuPort {
        private final Map<Long, Sku> store = new HashMap<>();

        void put(Sku sku) { store.put(sku.getId(), sku); }

        @Override public Optional<Sku> findSkuById(Long id)               { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Sku> findBySkuCode(String code)         { return Optional.empty(); }
        @Override public List<Sku>     findSkusByVariantId(Long id)       { return List.of(); }
        @Override public List<Sku>     findAllByIds(Collection<Long> ids) { return List.of(); }
    }
}
