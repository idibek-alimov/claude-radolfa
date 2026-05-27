package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.warehouse.RelocateStockUseCase;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RelocateStockServiceTest {

    static final Long SKU_ID   = 1L;
    static final Long FROM_BIN = 10L;
    static final Long TO_BIN   = 20L;
    static final Long ACTOR    = 99L;

    // ── Fakes ──────────────────────────────────────────────────────────────────

    static class FakeRecordInventoryTransactionPort implements RecordInventoryTransactionPort {
        final List<InventoryTransaction> recorded = Collections.synchronizedList(new ArrayList<>());
        @Override public void record(InventoryTransaction t) { recorded.add(t); }
    }

    /**
     * Tracks bin quantities. Throws InsufficientPlacementStockException when source bin
     * has fewer units than requested, and BinWarehouseMismatchException for a configured
     * foreign bin id.
     */
    static class FakePlacementPort implements InventoryPlacementPort {
        final Map<Long, Integer> bins; // binId → qty
        final Long foreignBinId;

        FakePlacementPort(Map<Long, Integer> bins, Long foreignBinId) {
            this.bins        = new HashMap<>(bins);
            this.foreignBinId = foreignBinId;
        }

        @Override
        public void relocate(Long skuId, Long warehouseId, Long fromBinId, Long toBinId, int qty) {
            if (foreignBinId != null && (foreignBinId.equals(fromBinId) || foreignBinId.equals(toBinId))) {
                throw new BinWarehouseMismatchException(foreignBinId, warehouseId);
            }
            int available = bins.getOrDefault(fromBinId, 0);
            if (available < qty) {
                throw new InsufficientPlacementStockException(skuId, fromBinId, available, qty);
            }
            bins.merge(fromBinId, -qty, Integer::sum);
            bins.merge(toBinId, qty, Integer::sum);
        }

        @Override public void addToInbound(Long s, Long w, int q)               {}
        @Override public boolean decrementForSale(Long s, Long w, int q)        { return true; }
        @Override public void putaway(Long s, Long w, Long b, int q)            {}
        @Override public void adjustInbound(Long s, Long w, int d)              {}
        @Override public int totalForSku(Long s, Long w)                        { return 0; }
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
    @DisplayName("Happy path: bin→bin move, RELOCATION ledger row with delta 0")
    void relocate_success_recordsLedgerRow() {
        var placement = new FakePlacementPort(Map.of(FROM_BIN, 40), null);
        var ledger    = new FakeRecordInventoryTransactionPort();
        var svc       = new RelocateStockService(placement, ledger, FAKE_WAREHOUSE);

        svc.execute(new RelocateStockUseCase.Command(SKU_ID, FROM_BIN, TO_BIN, 15, ACTOR));

        assertEquals(25, placement.bins.get(FROM_BIN), "source bin decremented");
        assertEquals(15, placement.bins.get(TO_BIN), "target bin incremented");

        assertEquals(1, ledger.recorded.size());
        InventoryTransaction tx = ledger.recorded.get(0);
        assertEquals(InventoryTransactionType.RELOCATION, tx.type());
        assertEquals(0, tx.delta());
        assertEquals("BIN", tx.referenceType());
        assertEquals(TO_BIN, tx.referenceId());
        assertEquals(SKU_ID, tx.skuId());
        assertEquals(ACTOR, tx.actorUserId());
    }

    @Test
    @DisplayName("Insufficient source bin → InsufficientPlacementStockException, no ledger row")
    void relocate_insufficientSource_throwsAndNoLedger() {
        var placement = new FakePlacementPort(Map.of(FROM_BIN, 5), null);
        var ledger    = new FakeRecordInventoryTransactionPort();
        var svc       = new RelocateStockService(placement, ledger, FAKE_WAREHOUSE);

        assertThrows(InsufficientPlacementStockException.class,
                () -> svc.execute(new RelocateStockUseCase.Command(SKU_ID, FROM_BIN, TO_BIN, 10, ACTOR)));
        assertTrue(ledger.recorded.isEmpty(), "no ledger row on failure");
    }

    @Test
    @DisplayName("Foreign-warehouse bin → BinWarehouseMismatchException, no ledger row")
    void relocate_foreignWarehouseBin_throwsAndNoLedger() {
        Long foreign  = 999L;
        var placement = new FakePlacementPort(Map.of(FROM_BIN, 50), foreign);
        var ledger    = new FakeRecordInventoryTransactionPort();
        var svc       = new RelocateStockService(placement, ledger, FAKE_WAREHOUSE);

        assertThrows(BinWarehouseMismatchException.class,
                () -> svc.execute(new RelocateStockUseCase.Command(SKU_ID, foreign, TO_BIN, 10, ACTOR)));
        assertTrue(ledger.recorded.isEmpty(), "no ledger row on failure");
    }
}
