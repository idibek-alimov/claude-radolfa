package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.AtomicStockPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.InsufficientStockException;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    private InMemoryAtomicStockPort          atomicStockPort;
    private InMemoryLoadSkuPort              loadSkuPort;
    private FakeRecordInventoryTransactionPort ledgerPort;
    private UpdateProductStockService        service;

    @BeforeEach
    void setUp() {
        atomicStockPort = new InMemoryAtomicStockPort();
        loadSkuPort     = new InMemoryLoadSkuPort();
        ledgerPort      = new FakeRecordInventoryTransactionPort();
        service = new UpdateProductStockService(loadSkuPort, NO_SAVE, atomicStockPort, ledgerPort);
    }

    // ── decrement (legacy signature) ──────────────────────────────────────────

    @Test
    void decrement_happyPath_stockReduced() {
        atomicStockPort.put(SKU_ID, 5);

        service.decrement(SKU_ID, 3);

        assertEquals(2, atomicStockPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        assertEquals(InventoryTransactionType.SALE, ledgerPort.recorded.get(0).type());
        assertEquals(-3, ledgerPort.recorded.get(0).delta());
    }

    @Test
    void decrement_exactStock_stockBecomesZero() {
        atomicStockPort.put(SKU_ID, 2);

        service.decrement(SKU_ID, 2);

        assertEquals(0, atomicStockPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
    }

    @Test
    void decrement_insufficientStock_throwsInsufficientStockException_andNoLedgerRow() {
        atomicStockPort.put(SKU_ID, 1);
        loadSkuPort.put(sku(SKU_ID, 1));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> service.decrement(SKU_ID, 5)
        );

        assertEquals(SKU_ID, ex.getSkuId());
        assertEquals(1, ex.getAvailable());
        assertEquals(5, ex.getRequested());
        assertEquals(1, atomicStockPort.stockOf(SKU_ID));
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
        atomicStockPort.put(SKU_ID, 10);

        service.decrement(SKU_ID, 3, ORDER_ID, USER_ID);

        assertEquals(7, atomicStockPort.stockOf(SKU_ID));
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
        atomicStockPort.put(SKU_ID, 1);
        loadSkuPort.put(sku(SKU_ID, 1));

        assertThrows(InsufficientStockException.class,
                () -> service.decrement(SKU_ID, 5, ORDER_ID, USER_ID));

        assertTrue(ledgerPort.recorded.isEmpty());
    }

    // ── increment (legacy signature) ──────────────────────────────────────────

    @Test
    void increment_happyPath_stockIncreased() {
        atomicStockPort.put(SKU_ID, 3);

        service.increment(SKU_ID, 7);

        assertEquals(10, atomicStockPort.stockOf(SKU_ID));
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

    // ── increment (context-aware overload) ────────────────────────────────────

    @Test
    @DisplayName("increment with CANCELLATION type records correct ledger row")
    void increment_withContext_recordsCorrectType() {
        atomicStockPort.put(SKU_ID, 5);

        service.increment(SKU_ID, 2, InventoryTransactionType.CANCELLATION, "ORDER", ORDER_ID, USER_ID);

        assertEquals(7, atomicStockPort.stockOf(SKU_ID));
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
        atomicStockPort.put(SKU_ID, 5);

        service.increment(SKU_ID, 3, InventoryTransactionType.RECALL_RETURN, "ORDER", ORDER_ID, USER_ID);

        assertEquals(8, atomicStockPort.stockOf(SKU_ID));
        assertEquals(1, ledgerPort.recorded.size());
        assertEquals(InventoryTransactionType.RECALL_RETURN, ledgerPort.recorded.get(0).type());
    }

    // ── setAbsolute ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setAbsolute raises stock → records MANUAL_ADJUSTMENT with positive delta")
    void setAbsolute_increaseStock_recordsManualAdjustment() {
        loadSkuPort.put(sku(SKU_ID, 7));

        service.setAbsolute(SKU_ID, 10, USER_ID);

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
        loadSkuPort.put(sku(SKU_ID, 10));

        service.setAbsolute(SKU_ID, 3, USER_ID);

        assertEquals(1, ledgerPort.recorded.size());
        assertEquals(-7, ledgerPort.recorded.get(0).delta());
    }

    @Test
    @DisplayName("setAbsolute to same value → no ledger row recorded")
    void setAbsolute_noDelta_noLedgerRow() {
        loadSkuPort.put(sku(SKU_ID, 7));

        service.setAbsolute(SKU_ID, 7, USER_ID);

        assertTrue(ledgerPort.recorded.isEmpty(), "delta=0 must not write a ledger row");
    }

    // ── concurrency ───────────────────────────────────────────────────────────

    @Test
    void decrement_concurrent_neverOversells() throws InterruptedException {
        int initialStock  = 500;
        int threads       = 1000;
        atomicStockPort.put(SKU_ID, initialStock);
        loadSkuPort.put(sku(SKU_ID, 0)); // used only on error path

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
        assertEquals(0, atomicStockPort.stockOf(SKU_ID), "Stock must reach exactly 0");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Sku sku(Long id, int stock) {
        return new Sku(id, VARIANT_ID, "SKU-" + id, "M", stock, new Money(BigDecimal.TEN));
    }

    // ── in-memory fakes ───────────────────────────────────────────────────────

    static class FakeRecordInventoryTransactionPort implements RecordInventoryTransactionPort {
        final List<InventoryTransaction> recorded = new ArrayList<>();

        @Override
        public void record(InventoryTransaction transaction) {
            recorded.add(transaction);
        }
    }

    static class InMemoryAtomicStockPort implements AtomicStockPort {
        private final ConcurrentHashMap<Long, AtomicInteger> store = new ConcurrentHashMap<>();

        void put(Long skuId, int stock) { store.put(skuId, new AtomicInteger(stock)); }
        int stockOf(Long skuId)         { return store.getOrDefault(skuId, new AtomicInteger(0)).get(); }

        @Override
        public int decrementIfAvailable(Long skuId, int qty) {
            AtomicInteger stock = store.get(skuId);
            if (stock == null) return 0;
            while (true) {
                int current = stock.get();
                if (current < qty) return 0;
                if (stock.compareAndSet(current, current - qty)) return 1;
            }
        }

        @Override
        public int increment(Long skuId, int qty) {
            AtomicInteger stock = store.get(skuId);
            if (stock == null) return 0;
            stock.addAndGet(qty);
            return 1;
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

    static final SaveProductHierarchyPort NO_SAVE = new SaveProductHierarchyPort() {
        @Override public tj.radolfa.domain.model.ProductBase    saveBase(tj.radolfa.domain.model.ProductBase b)                 { return b; }
        @Override public tj.radolfa.domain.model.ListingVariant saveVariant(tj.radolfa.domain.model.ListingVariant v, Long pid) { return v; }
        @Override public Sku                                     saveSku(Sku s, Long vid)                                        { return s; }
    };
}
