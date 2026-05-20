package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.CreateStockReceiptUseCase;
import tj.radolfa.application.ports.in.warehouse.CreateStockReceiptUseCase.Command;
import tj.radolfa.application.ports.in.warehouse.CreateStockReceiptUseCase.ItemCommand;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadProductBasePort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.SaveStockReceiptPort;
import tj.radolfa.application.ports.out.StockAdjustmentPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.StockReceipt;
import tj.radolfa.domain.model.StockReceiptItem;
import tj.radolfa.domain.model.StockReceiptStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CreateStockReceiptServiceTest {

    static final Long ADMIN_ID      = 99L;
    static final Long SKU_A_ID      = 10L;
    static final Long SKU_B_ID      = 11L;
    static final Long VARIANT_A_ID  = 100L;
    static final Long VARIANT_B_ID  = 101L;
    static final Long PRODUCT_A_ID  = 200L;
    static final Long PRODUCT_B_ID  = 201L;

    // ── Domain fixtures ───────────────────────────────────────────────────────

    static Sku sku(Long id, Long variantId, String code) {
        return new Sku(id, variantId, code, "M", 5, new Money(BigDecimal.TEN));
    }

    static ListingVariant variant(Long id, Long productBaseId) {
        return new ListingVariant(id, productBaseId, "black", "slug-" + id,
                null, List.of(), List.of(), List.of(), null, null,
                true, true, null, null, null, null);
    }

    static ProductBase productBase(Long id, String name) {
        return new ProductBase(id, "EXT-" + id, name, "Category", null, null);
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeSaveStockReceiptPort implements SaveStockReceiptPort {
        StockReceipt saved;
        private long nextId = 1L;

        @Override
        public StockReceipt save(StockReceipt receipt) {
            Long receiptId = nextId++;
            List<StockReceiptItem> itemsWithIds = new ArrayList<>();
            for (StockReceiptItem item : receipt.getItems()) {
                itemsWithIds.add(new StockReceiptItem(nextId++, receiptId, item.skuId(),
                        item.skuCode(), item.productName(), item.quantityReceived(), item.notes()));
            }
            saved = new StockReceipt(receiptId, receipt.getCreatedByUserId(), receipt.getCreatedAt(),
                    receipt.getSupplierReference(), receipt.getNotes(), receipt.getStatus(), itemsWithIds);
            return saved;
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

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        final Map<Long, ListingVariant> store;
        FakeLoadListingVariantPort(Map<Long, ListingVariant> store) { this.store = store; }

        @Override public Optional<ListingVariant>   findVariantById(Long id)                               { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<ListingVariant>   findByProductBaseIdAndColorKey(Long pid, String ck)    { return Optional.empty(); }
        @Override public Optional<ListingVariant>   findBySlug(String slug)                                { return Optional.empty(); }
        @Override public List<ListingVariant>       findAllByProductBaseId(Long productBaseId)             { return List.of(); }
        @Override public Map<Long, ListingVariant>  findVariantsByIds(Collection<Long> ids) {
            return ids.stream().filter(store::containsKey).collect(Collectors.toMap(id -> id, store::get));
        }
    }

    static class FakeLoadProductBasePort implements LoadProductBasePort {
        final Map<Long, ProductBase> store;
        FakeLoadProductBasePort(Map<Long, ProductBase> store) { this.store = store; }

        @Override public Optional<ProductBase>  findByExternalRef(String ref)         { return Optional.empty(); }
        @Override public Optional<ProductBase>  findById(Long id)                     { return Optional.ofNullable(store.get(id)); }
        @Override public Map<Long, ProductBase> findProductsByIds(Collection<Long> ids) {
            return ids.stream().filter(store::containsKey).collect(Collectors.toMap(id -> id, store::get));
        }
    }

    static class CapturingStockAdjustmentPort implements StockAdjustmentPort {
        record IncrementCall(Long skuId, int qty, InventoryTransactionType type,
                             String referenceType, Long referenceId, Long actorUserId) {}
        final List<IncrementCall> calls = new ArrayList<>();

        @Override public void decrement(Long skuId, int qty) {}
        @Override public void increment(Long skuId, int qty) {}
        @Override public void setAbsolute(Long skuId, int qty) {}

        @Override
        public void increment(Long skuId, int qty, InventoryTransactionType type,
                              String referenceType, Long referenceId, Long actorUserId) {
            calls.add(new IncrementCall(skuId, qty, type, referenceType, referenceId, actorUserId));
        }
    }

    CreateStockReceiptService service(FakeSaveStockReceiptPort save,
                                      FakeLoadSkuPort skuPort,
                                      FakeLoadListingVariantPort variantPort,
                                      FakeLoadProductBasePort productBasePort,
                                      CapturingStockAdjustmentPort stock) {
        return new CreateStockReceiptService(save, skuPort, variantPort, productBasePort, stock);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2-item receipt with valid SKUs → COMPLETED status, 2 stock increments")
    void twoItems_savedAsCompleted_twoIncrementsRecorded() {
        var save        = new FakeSaveStockReceiptPort();
        var stock       = new CapturingStockAdjustmentPort();
        var skus        = Map.of(SKU_A_ID, sku(SKU_A_ID, VARIANT_A_ID, "SKU-A"),
                                 SKU_B_ID, sku(SKU_B_ID, VARIANT_B_ID, "SKU-B"));
        var variants    = Map.of(VARIANT_A_ID, variant(VARIANT_A_ID, PRODUCT_A_ID),
                                 VARIANT_B_ID, variant(VARIANT_B_ID, PRODUCT_B_ID));
        var productBases = Map.of(PRODUCT_A_ID, productBase(PRODUCT_A_ID, "Widget"),
                                  PRODUCT_B_ID, productBase(PRODUCT_B_ID, "Gadget"));

        var svc = service(save, new FakeLoadSkuPort(skus),
                new FakeLoadListingVariantPort(variants), new FakeLoadProductBasePort(productBases), stock);

        StockReceipt result = svc.execute(new Command(ADMIN_ID, "INV-001", null,
                List.of(new ItemCommand(SKU_A_ID, 10, null), new ItemCommand(SKU_B_ID, 5, null))));

        assertEquals(StockReceiptStatus.COMPLETED, result.getStatus());
        assertEquals(2, result.getItems().size());
        assertEquals(2, stock.calls.size());
        stock.calls.forEach(c -> {
            assertEquals(InventoryTransactionType.RECEIPT, c.type());
            assertEquals("STOCK_RECEIPT", c.referenceType());
            assertEquals(result.getId(), c.referenceId());
            assertEquals(ADMIN_ID, c.actorUserId());
        });
    }

    @Test
    @DisplayName("productName resolved from productBase chain, not skuCode")
    void productName_resolvedFromProductBaseChain() {
        var save        = new FakeSaveStockReceiptPort();
        var stock       = new CapturingStockAdjustmentPort();
        var skus        = Map.of(SKU_A_ID, sku(SKU_A_ID, VARIANT_A_ID, "SKU-A"));
        var variants    = Map.of(VARIANT_A_ID, variant(VARIANT_A_ID, PRODUCT_A_ID));
        var productBases = Map.of(PRODUCT_A_ID, productBase(PRODUCT_A_ID, "Awesome Widget"));

        var svc = service(save, new FakeLoadSkuPort(skus),
                new FakeLoadListingVariantPort(variants), new FakeLoadProductBasePort(productBases), stock);

        svc.execute(new Command(ADMIN_ID, null, null, List.of(new ItemCommand(SKU_A_ID, 3, null))));

        assertEquals("Awesome Widget", save.saved.getItems().get(0).productName());
    }

    @Test
    @DisplayName("Unknown SKU id → ResourceNotFoundException, no receipt saved, no stock incremented")
    void unknownSkuId_throwsResourceNotFoundException() {
        var save  = new FakeSaveStockReceiptPort();
        var stock = new CapturingStockAdjustmentPort();
        var svc = service(save, new FakeLoadSkuPort(Map.of()),
                new FakeLoadListingVariantPort(Map.of()), new FakeLoadProductBasePort(Map.of()), stock);

        assertThrows(ResourceNotFoundException.class,
                () -> svc.execute(new Command(ADMIN_ID, null, null,
                        List.of(new ItemCommand(SKU_A_ID, 5, null)))));

        assertNull(save.saved);
        assertTrue(stock.calls.isEmpty());
    }

    @Test
    @DisplayName("Empty items list → IllegalArgumentException, no receipt saved")
    void emptyItems_throwsIllegalArgument() {
        var save = new FakeSaveStockReceiptPort();
        var svc = service(save, new FakeLoadSkuPort(Map.of()),
                new FakeLoadListingVariantPort(Map.of()), new FakeLoadProductBasePort(Map.of()),
                new CapturingStockAdjustmentPort());

        assertThrows(IllegalArgumentException.class,
                () -> svc.execute(new Command(ADMIN_ID, null, null, List.of())));

        assertNull(save.saved);
    }

    @Test
    @DisplayName("supplierReference null → allowed; receipt saved with COMPLETED status")
    void nullSupplierReference_savedNormally() {
        var save  = new FakeSaveStockReceiptPort();
        var skus  = Map.of(SKU_A_ID, sku(SKU_A_ID, VARIANT_A_ID, "SKU-A"));
        var variants = Map.of(VARIANT_A_ID, variant(VARIANT_A_ID, PRODUCT_A_ID));
        var productBases = Map.of(PRODUCT_A_ID, productBase(PRODUCT_A_ID, "Widget"));
        var svc = service(save, new FakeLoadSkuPort(skus),
                new FakeLoadListingVariantPort(variants), new FakeLoadProductBasePort(productBases),
                new CapturingStockAdjustmentPort());

        StockReceipt result = svc.execute(new Command(ADMIN_ID, null, null,
                List.of(new ItemCommand(SKU_A_ID, 1, null))));

        assertEquals(StockReceiptStatus.COMPLETED, result.getStatus());
        assertNull(result.getSupplierReference());
    }

    @Test
    @DisplayName("Ledger referenceId equals the saved receipt id, not null")
    void ledgerReferenceId_equalsSavedReceiptId() {
        var save  = new FakeSaveStockReceiptPort();
        var stock = new CapturingStockAdjustmentPort();
        var skus  = Map.of(SKU_A_ID, sku(SKU_A_ID, VARIANT_A_ID, "SKU-A"));
        var variants = Map.of(VARIANT_A_ID, variant(VARIANT_A_ID, PRODUCT_A_ID));
        var productBases = Map.of(PRODUCT_A_ID, productBase(PRODUCT_A_ID, "Widget"));
        var svc = service(save, new FakeLoadSkuPort(skus),
                new FakeLoadListingVariantPort(variants), new FakeLoadProductBasePort(productBases), stock);

        StockReceipt result = svc.execute(new Command(ADMIN_ID, "REF-X", null,
                List.of(new ItemCommand(SKU_A_ID, 7, null))));

        assertNotNull(result.getId());
        assertEquals(1, stock.calls.size());
        assertEquals(result.getId(), stock.calls.get(0).referenceId());
        assertEquals(7, stock.calls.get(0).qty());
    }
}
