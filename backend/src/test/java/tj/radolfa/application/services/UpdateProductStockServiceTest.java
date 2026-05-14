package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.AtomicStockPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveProductHierarchyPort;
import tj.radolfa.domain.exception.InsufficientStockException;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Sku;

import java.math.BigDecimal;
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

    private InMemoryAtomicStockPort atomicStockPort;
    private InMemoryLoadSkuPort     loadSkuPort;
    private UpdateProductStockService service;

    @BeforeEach
    void setUp() {
        atomicStockPort = new InMemoryAtomicStockPort();
        loadSkuPort     = new InMemoryLoadSkuPort();
        service = new UpdateProductStockService(loadSkuPort, NO_SAVE, atomicStockPort);
    }

    // ── decrement ──────────────────────────────────────────────────────────────

    @Test
    void decrement_happyPath_stockReduced() {
        atomicStockPort.put(SKU_ID, 5);

        service.decrement(SKU_ID, 3);

        assertEquals(2, atomicStockPort.stockOf(SKU_ID));
    }

    @Test
    void decrement_exactStock_stockBecomesZero() {
        atomicStockPort.put(SKU_ID, 2);

        service.decrement(SKU_ID, 2);

        assertEquals(0, atomicStockPort.stockOf(SKU_ID));
    }

    @Test
    void decrement_insufficientStock_throwsInsufficientStockException() {
        atomicStockPort.put(SKU_ID, 1);
        loadSkuPort.put(sku(SKU_ID, 1));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> service.decrement(SKU_ID, 5)
        );

        assertEquals(SKU_ID, ex.getSkuId());
        assertEquals(1, ex.getAvailable());
        assertEquals(5, ex.getRequested());
        // Stock must not have changed
        assertEquals(1, atomicStockPort.stockOf(SKU_ID));
    }

    @Test
    void decrement_zeroQuantity_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.decrement(SKU_ID, 0));
    }

    @Test
    void decrement_negativeQuantity_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.decrement(SKU_ID, -1));
    }

    // ── increment ─────────────────────────────────────────────────────────────

    @Test
    void increment_happyPath_stockIncreased() {
        atomicStockPort.put(SKU_ID, 3);

        service.increment(SKU_ID, 7);

        assertEquals(10, atomicStockPort.stockOf(SKU_ID));
    }

    @Test
    void increment_skuNotFound_throwsIllegalArgument() {
        // SKU not in store → increment returns 0
        assertThrows(IllegalArgumentException.class, () -> service.increment(99L, 1));
    }

    @Test
    void increment_zeroQuantity_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.increment(SKU_ID, 0));
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

    static class InMemoryAtomicStockPort implements AtomicStockPort {
        private final ConcurrentHashMap<Long, AtomicInteger> store = new ConcurrentHashMap<>();

        void put(Long skuId, int stock) { store.put(skuId, new AtomicInteger(stock)); }
        int stockOf(Long skuId)         { return store.getOrDefault(skuId, new AtomicInteger(0)).get(); }

        @Override
        public int decrementIfAvailable(Long skuId, int qty) {
            AtomicInteger stock = store.get(skuId);
            if (stock == null) return 0;
            // CAS loop — mirrors what the DB WHERE clause does atomically
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

        @Override public Optional<Sku> findSkuById(Long id)            { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Sku> findBySkuCode(String code)      { return Optional.empty(); }
        @Override public List<Sku>     findSkusByVariantId(Long id)    { return List.of(); }
        @Override public List<Sku>     findAllByIds(Collection<Long> ids) { return List.of(); }
    }

    static final SaveProductHierarchyPort NO_SAVE = new SaveProductHierarchyPort() {
        @Override public tj.radolfa.domain.model.ProductBase     saveBase(tj.radolfa.domain.model.ProductBase b)                    { return b; }
        @Override public tj.radolfa.domain.model.ListingVariant  saveVariant(tj.radolfa.domain.model.ListingVariant v, Long pid)    { return v; }
        @Override public Sku                                      saveSku(Sku s, Long vid)                                           { return s; }
    };
}
