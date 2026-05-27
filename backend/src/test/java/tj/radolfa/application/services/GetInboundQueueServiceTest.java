package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.PageResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetInboundQueueServiceTest {

    // ── Fake ──────────────────────────────────────────────────────────────────

    static class CapturingPlacementPort implements InventoryPlacementPort {
        record QueueCall(int page, int size, String search) {}
        final List<QueueCall> calls = new ArrayList<>();
        PageResult<InboundQueueItem> result = new PageResult<>(List.of(), 0, 1, 20, true);

        @Override
        public PageResult<InboundQueueItem> findInboundQueue(int page, int size, String search) {
            calls.add(new QueueCall(page, size, search));
            return result;
        }

        @Override public void addToInbound(Long s, Long w, int q)               {}
        @Override public boolean decrementForSale(Long s, Long w, int q)        { return true; }
        @Override public void putaway(Long s, Long w, Long b, int q)            {}
        @Override public void relocate(Long s, Long w, Long f, Long t, int q)   {}
        @Override public void adjustInbound(Long s, Long w, int d)              {}
        @Override public int totalForSku(Long s, Long w)                        { return 0; }
        @Override public List<InventoryPlacement> placementsForSku(Long s, Long w) { return List.of(); }
        @Override public boolean hasPlacementsInBin(Long binId)                 { return false; }
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Delegates page, size, and search to the port unchanged")
    void delegatesParamsToPort() {
        var port = new CapturingPlacementPort();
        var svc  = new GetInboundQueueService(port);

        svc.execute(3, 10, "SKU-ABC");

        assertEquals(1, port.calls.size());
        var call = port.calls.get(0);
        assertEquals(3, call.page());
        assertEquals(10, call.size());
        assertEquals("SKU-ABC", call.search());
    }

    @Test
    @DisplayName("Pagination metadata is echoed from the port result")
    void paginationMetadataEchoed() {
        var port = new CapturingPlacementPort();
        port.result = new PageResult<>(List.of(), 55L, 2, 15, false);

        var svc = new GetInboundQueueService(port);
        PageResult<InboundQueueItem> result = svc.execute(2, 15, "");

        assertEquals(55L, result.totalElements());
        assertEquals(2, result.number());
        assertEquals(15, result.size());
        assertFalse(result.last());
    }

    @Test
    @DisplayName("Items returned by the port are passed through unchanged")
    void itemsPassedThrough() {
        var item = new InboundQueueItem(7L, "SKU-007", "BAR007", "Product A", 42);
        var port = new CapturingPlacementPort();
        port.result = new PageResult<>(List.of(item), 1L, 1, 20, true);

        var svc = new GetInboundQueueService(port);
        var result = svc.execute(1, 20, null);

        assertEquals(1, result.content().size());
        assertEquals(item, result.content().get(0));
    }
}
