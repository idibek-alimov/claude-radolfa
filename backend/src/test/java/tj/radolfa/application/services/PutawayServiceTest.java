package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.PutawayUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.exception.BinWarehouseMismatchException;
import tj.radolfa.domain.exception.InsufficientPlacementStockException;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Warehouse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PutawayServiceTest {

    static final Long SKU_ID  = 1L;
    static final Long BIN_ID  = 10L;
    static final Long ACTOR   = 99L;

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
    }

    static final LoadWarehousePort FAKE_WAREHOUSE = new LoadWarehousePort() {
        @Override public Warehouse findDefault() {
            return new Warehouse(1L, "MAIN", "Main Warehouse", true, Instant.now());
        }
        @Override public Optional<Warehouse> findById(Long id) {
            return id == 1L ? Optional.of(findDefault()) : Optional.empty();
        }
    };

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Happy path: inbound → bin, PUTAWAY ledger row with delta 0")
    void putaway_success_recordsLedgerRow() {
        var placementPort = new FakePlacementPort(50, null);
        var ledger        = new FakeRecordInventoryTransactionPort();
        var svc           = new PutawayService(placementPort, ledger, FAKE_WAREHOUSE);

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
    }

    @Test
    @DisplayName("Insufficient inbound → InsufficientPlacementStockException, no ledger row")
    void putaway_insufficientInbound_throwsAndNoLedger() {
        var placementPort = new FakePlacementPort(5, null);
        var ledger        = new FakeRecordInventoryTransactionPort();
        var svc           = new PutawayService(placementPort, ledger, FAKE_WAREHOUSE);

        assertThrows(InsufficientPlacementStockException.class,
                () -> svc.execute(new PutawayUseCase.Command(SKU_ID, BIN_ID, 10, ACTOR)));
        assertTrue(ledger.recorded.isEmpty(), "no ledger row on failure");
    }

    @Test
    @DisplayName("Foreign-warehouse bin → BinWarehouseMismatchException, no ledger row")
    void putaway_binInWrongWarehouse_throwsAndNoLedger() {
        Long foreignBin   = 999L;
        var placementPort = new FakePlacementPort(50, foreignBin);
        var ledger        = new FakeRecordInventoryTransactionPort();
        var svc           = new PutawayService(placementPort, ledger, FAKE_WAREHOUSE);

        assertThrows(BinWarehouseMismatchException.class,
                () -> svc.execute(new PutawayUseCase.Command(SKU_ID, foreignBin, 10, ACTOR)));
        assertTrue(ledger.recorded.isEmpty(), "no ledger row on failure");
    }
}
