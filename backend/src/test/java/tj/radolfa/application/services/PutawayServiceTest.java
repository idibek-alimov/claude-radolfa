package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;
import tj.radolfa.application.event.ProductActivatedEvent;
import tj.radolfa.application.ports.in.warehouse.PutawayUseCase;
import tj.radolfa.application.ports.out.ActivateProductPort;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadProductForSkuPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.exception.BinWarehouseMismatchException;
import tj.radolfa.domain.exception.InsufficientPlacementStockException;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.domain.model.Warehouse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PutawayServiceTest {

    static final Long SKU_ID       = 1L;
    static final Long BIN_ID       = 10L;
    static final Long ACTOR        = 99L;
    static final Long PRODUCT_BASE = 42L;

    // ── Fakes ──────────────────────────────────────────────────────────────────

    static class FakeRecordInventoryTransactionPort implements RecordInventoryTransactionPort {
        final List<InventoryTransaction> recorded = Collections.synchronizedList(new ArrayList<>());
        @Override public void record(InventoryTransaction t) { recorded.add(t); }
    }

    /**
     * Simulates the adapter's validation: tracks inbound qty, throws on insufficient
     * stock or on a configurable "foreign" bin id.
     */
    static class FakePlacementPort implements InventoryPlacementPort {
        int inbound;
        final Long foreignBinId; // putaway/relocate to this bin id → mismatch

        FakePlacementPort(int inbound, Long foreignBinId) {
            this.inbound = inbound;
            this.foreignBinId = foreignBinId;
        }

        @Override
        public void putaway(Long skuId, Long warehouseId, Long binId, int qty) {
            if (foreignBinId != null && foreignBinId.equals(binId)) {
                throw new BinWarehouseMismatchException(binId, warehouseId);
            }
            if (inbound < qty) {
                throw new InsufficientPlacementStockException(skuId, null, inbound, qty);
            }
            inbound -= qty;
        }

        @Override public void addToInbound(Long s, Long w, int q)               {}
        @Override public boolean decrementForSale(Long s, Long w, int q)        { return true; }
        @Override public void relocate(Long s, Long w, Long f, Long t, int q)   {}
        @Override public void adjustInbound(Long s, Long w, int d)              {}
        @Override public int totalForSku(Long s, Long w)                        { return inbound; }
        @Override public List<InventoryPlacement> placementsForSku(Long s, Long w) { return List.of(); }
        @Override public PageResult<InboundQueueItem> findInboundQueue(int p, int sz, String q) {
            return new PageResult<>(List.of(), 0, p, sz, true);
        }
        @Override public boolean hasPlacementsInBin(Long binId)                 { return false; }
        @Override public List<tj.radolfa.domain.model.PlacementView> placementViewsForSku(Long s, Long w)        { return List.of(); }
        @Override public java.util.Map<Long, List<tj.radolfa.domain.model.PlacementView>> placementViewsForSkus(
                java.util.Collection<Long> ids, Long w)                                                          { return java.util.Map.of(); }
    }

    static final LoadWarehousePort FAKE_WAREHOUSE = new LoadWarehousePort() {
        @Override public Warehouse findDefault() {
            return new Warehouse(1L, "MAIN", "Main Warehouse", true, Instant.now());
        }
        @Override public Optional<Warehouse> findById(Long id) {
            return id == 1L ? Optional.of(findDefault()) : Optional.empty();
        }
    };

    static class FakeLoadProductForSkuPort implements LoadProductForSkuPort {
        final Map<Long, Long> skuToBase;
        FakeLoadProductForSkuPort(Map<Long, Long> skuToBase) { this.skuToBase = skuToBase; }
        @Override public Optional<Long> findProductBaseIdBySkuId(Long skuId) {
            return Optional.ofNullable(skuToBase.get(skuId));
        }
    }

    static final FakeLoadProductForSkuPort EMPTY_SKU_PORT = new FakeLoadProductForSkuPort(Map.of());

    static class FakeActivateProductPort implements ActivateProductPort {
        final Map<Long, ProductStatus> baseStatus;
        FakeActivateProductPort(Map<Long, ProductStatus> baseStatus) {
            this.baseStatus = new HashMap<>(baseStatus);
        }
        @Override
        public int activateIfAwaitingStock(Long productBaseId) {
            if (baseStatus.get(productBaseId) == ProductStatus.AWAITING_STOCK) {
                baseStatus.put(productBaseId, ProductStatus.ACTIVE);
                return 1;
            }
            return 0;
        }
    }

    static final ActivateProductPort NOOP_ACTIVATE = new FakeActivateProductPort(Map.of());

    static class RecordingEventPublisher implements ApplicationEventPublisher {
        final List<Object> events = new ArrayList<>();
        @Override public void publishEvent(Object event) { events.add(event); }
    }

    static final RecordingEventPublisher NO_OP_PUBLISHER = new RecordingEventPublisher();

    // ── Helpers ────────────────────────────────────────────────────────────────

    static PutawayService build(FakePlacementPort p, FakeRecordInventoryTransactionPort l,
                                FakeLoadProductForSkuPort skuPort, FakeActivateProductPort activate,
                                RecordingEventPublisher publisher) {
        return new PutawayService(p, l, FAKE_WAREHOUSE, skuPort, activate, publisher);
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Happy path: inbound → bin, PUTAWAY ledger row with delta 0")
    void putaway_success_recordsLedgerRow() {
        var placementPort = new FakePlacementPort(50, null);
        var ledger        = new FakeRecordInventoryTransactionPort();
        var publisher     = new RecordingEventPublisher();
        var svc           = build(placementPort, ledger, EMPTY_SKU_PORT,
                                  new FakeActivateProductPort(Map.of()), publisher);

        svc.execute(new PutawayUseCase.Command(SKU_ID, BIN_ID, 20, ACTOR));

        assertEquals(30, placementPort.inbound, "inbound should drop by 20");
        assertEquals(1, ledger.recorded.size());
        InventoryTransaction tx = ledger.recorded.get(0);
        assertEquals(InventoryTransactionType.PUTAWAY, tx.type());
        assertEquals(0, tx.delta());
        assertEquals("BIN", tx.referenceType());
        assertEquals(BIN_ID, tx.referenceId());
        assertEquals(SKU_ID, tx.skuId());
        assertEquals(ACTOR, tx.actorUserId());
        assertTrue(publisher.events.isEmpty(), "no event when SKU has no product base mapping");
    }

    @Test
    @DisplayName("Insufficient inbound → InsufficientPlacementStockException, no ledger row")
    void putaway_insufficientInbound_throwsAndNoLedger() {
        var placementPort = new FakePlacementPort(5, null);
        var ledger        = new FakeRecordInventoryTransactionPort();
        var publisher     = new RecordingEventPublisher();
        var svc           = build(placementPort, ledger, EMPTY_SKU_PORT,
                                  new FakeActivateProductPort(Map.of()), publisher);

        assertThrows(InsufficientPlacementStockException.class,
                () -> svc.execute(new PutawayUseCase.Command(SKU_ID, BIN_ID, 10, ACTOR)));
        assertTrue(ledger.recorded.isEmpty(), "no ledger row on failure");
        assertTrue(publisher.events.isEmpty(), "no event on failure");
    }

    @Test
    @DisplayName("Foreign-warehouse bin → BinWarehouseMismatchException, no ledger row")
    void putaway_binInWrongWarehouse_throwsAndNoLedger() {
        Long foreignBin   = 999L;
        var placementPort = new FakePlacementPort(50, foreignBin);
        var ledger        = new FakeRecordInventoryTransactionPort();
        var publisher     = new RecordingEventPublisher();
        var svc           = build(placementPort, ledger, EMPTY_SKU_PORT,
                                  new FakeActivateProductPort(Map.of()), publisher);

        assertThrows(BinWarehouseMismatchException.class,
                () -> svc.execute(new PutawayUseCase.Command(SKU_ID, foreignBin, 10, ACTOR)));
        assertTrue(ledger.recorded.isEmpty(), "no ledger row on failure");
        assertTrue(publisher.events.isEmpty(), "no event on failure");
    }

    @Test
    @DisplayName("First putaway for AWAITING_STOCK product flips status to ACTIVE and publishes event")
    void putaway_flipsAwaitingStockToActive_andPublishesEvent() {
        var skuPort   = new FakeLoadProductForSkuPort(Map.of(SKU_ID, PRODUCT_BASE));
        var activate  = new FakeActivateProductPort(Map.of(PRODUCT_BASE, ProductStatus.AWAITING_STOCK));
        var publisher = new RecordingEventPublisher();
        var svc       = build(new FakePlacementPort(50, null),
                              new FakeRecordInventoryTransactionPort(),
                              skuPort, activate, publisher);

        svc.execute(new PutawayUseCase.Command(SKU_ID, BIN_ID, 10, ACTOR));

        assertEquals(ProductStatus.ACTIVE, activate.baseStatus.get(PRODUCT_BASE));
        assertEquals(1, publisher.events.size());
        assertEquals(new ProductActivatedEvent(PRODUCT_BASE), publisher.events.get(0));
    }

    @Test
    @DisplayName("Second putaway on already-ACTIVE product: no flip, no event")
    void putaway_onAlreadyActiveBase_noFlipNoEvent() {
        var skuPort   = new FakeLoadProductForSkuPort(Map.of(SKU_ID, PRODUCT_BASE));
        var activate  = new FakeActivateProductPort(Map.of(PRODUCT_BASE, ProductStatus.ACTIVE));
        var publisher = new RecordingEventPublisher();
        var svc       = build(new FakePlacementPort(50, null),
                              new FakeRecordInventoryTransactionPort(),
                              skuPort, activate, publisher);

        svc.execute(new PutawayUseCase.Command(SKU_ID, BIN_ID, 10, ACTOR));

        assertEquals(ProductStatus.ACTIVE, activate.baseStatus.get(PRODUCT_BASE), "status unchanged");
        assertTrue(publisher.events.isEmpty(), "no duplicate event on second putaway");
    }

    @ParameterizedTest(name = "Putaway on {0} product: no flip, no event (defence-in-depth)")
    @EnumSource(value = ProductStatus.class, names = {"DRAFT", "PENDING_REVIEW", "REJECTED"})
    void putaway_onNonActivatableStatus_noFlipNoEvent(ProductStatus status) {
        var skuPort   = new FakeLoadProductForSkuPort(Map.of(SKU_ID, PRODUCT_BASE));
        var activate  = new FakeActivateProductPort(Map.of(PRODUCT_BASE, status));
        var publisher = new RecordingEventPublisher();
        var svc       = build(new FakePlacementPort(50, null),
                              new FakeRecordInventoryTransactionPort(),
                              skuPort, activate, publisher);

        svc.execute(new PutawayUseCase.Command(SKU_ID, BIN_ID, 10, ACTOR));

        assertEquals(status, activate.baseStatus.get(PRODUCT_BASE), "status must not change");
        assertTrue(publisher.events.isEmpty(), "no event for non-activatable status");
    }
}
