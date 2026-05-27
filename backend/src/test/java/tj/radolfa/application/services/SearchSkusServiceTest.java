package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.SearchSkusPort;
import tj.radolfa.application.readmodel.InboundQueueItem;
import tj.radolfa.domain.model.InventoryPlacement;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.PlacementView;
import tj.radolfa.domain.model.SkuSearchRow;
import tj.radolfa.domain.model.Warehouse;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SearchSkusServiceTest {

    static final Long WH_ID = 1L;

    static SkuSearchRow row(Long skuId, String code, String productName) {
        return new SkuSearchRow(skuId, code, "BAR-" + code, "M", 10, productName, List.of());
    }

    static SkuSearchRow row(String code, String productName) {
        return row(1L, code, productName);
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    static class FakeSearchSkusPort implements SearchSkusPort {
        List<SkuSearchRow> rows = List.of();
        long total = 0;
        String lastQuery;

        @Override
        public PageResult<SkuSearchRow> search(String query, int page, int size) {
            lastQuery = query;
            boolean isLast = (long) page * size >= total;
            return new PageResult<>(rows, total, page, size, isLast);
        }
    }

    static class FakePlacementPort implements InventoryPlacementPort {
        final Map<Long, List<PlacementView>> views;
        FakePlacementPort(Map<Long, List<PlacementView>> views) { this.views = views; }

        @Override public List<PlacementView> placementViewsForSku(Long s, Long w)        { return views.getOrDefault(s, List.of()); }
        @Override public Map<Long, List<PlacementView>> placementViewsForSkus(Collection<Long> ids, Long w) { return views; }
        @Override public void addToInbound(Long s, Long w, int q)               {}
        @Override public boolean decrementForSale(Long s, Long w, int q)        { return true; }
        @Override public void putaway(Long s, Long w, Long b, int q)            {}
        @Override public void relocate(Long s, Long w, Long f, Long t, int q)   {}
        @Override public void adjustInbound(Long s, Long w, int d)              {}
        @Override public int totalForSku(Long s, Long w)                        { return 0; }
        @Override public List<InventoryPlacement> placementsForSku(Long s, Long w) { return List.of(); }
        @Override public PageResult<InboundQueueItem> findInboundQueue(int p, int sz, String q) {
            return new PageResult<>(List.of(), 0, p, sz, true);
        }
        @Override public boolean hasPlacementsInBin(Long binId) { return false; }
    }

    static class FakeLoadWarehousePort implements LoadWarehousePort {
        @Override public Warehouse findDefault() {
            return new Warehouse(WH_ID, "MAIN", "Main Warehouse", true, Instant.now());
        }
        @Override public Optional<Warehouse> findById(Long id) {
            return id.equals(WH_ID) ? Optional.of(findDefault()) : Optional.empty();
        }
    }

    static final FakePlacementPort NO_PLACEMENTS = new FakePlacementPort(Map.of());

    SearchSkusService service(FakeSearchSkusPort port) {
        return new SearchSkusService(port, NO_PLACEMENTS, new FakeLoadWarehousePort());
    }

    SearchSkusService service(FakeSearchSkusPort port, FakePlacementPort placementPort) {
        return new SearchSkusService(port, placementPort, new FakeLoadWarehousePort());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("blank query returns empty page without calling the port")
    void blankQueryReturnsEmpty() {
        var port = new FakeSearchSkusPort();
        var result = service(port).execute("  ", 1, 20);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertTrue(result.last());
        assertNull(port.lastQuery);
    }

    @Test
    @DisplayName("null query returns empty page without calling the port")
    void nullQueryReturnsEmpty() {
        var port = new FakeSearchSkusPort();
        var result = service(port).execute(null, 1, 20);

        assertTrue(result.content().isEmpty());
        assertNull(port.lastQuery);
    }

    @Test
    @DisplayName("non-blank query is trimmed and forwarded to port")
    void queryTrimmedAndForwarded() {
        var port = new FakeSearchSkusPort();
        port.rows = List.of(row("SKU-001", "Shirt"));
        port.total = 1;

        var result = service(port).execute("  Shirt  ", 1, 20);

        assertEquals("Shirt", port.lastQuery);
        assertEquals(1, result.content().size());
        assertEquals("SKU-001", result.content().get(0).skuCode());
    }

    @Test
    @DisplayName("page=0 is clamped to 1 and forwarded to port")
    void pageZeroClamped() {
        var port = new FakeSearchSkusPort();
        port.rows = List.of(row("SKU-001", "Shirt"));
        port.total = 1;

        var result = service(port).execute("foo", 0, 20);

        assertEquals("foo", port.lastQuery);
        assertEquals(1, result.number());
    }

    @Test
    @DisplayName("pagination metadata is forwarded correctly")
    void paginationMetadata() {
        var port = new FakeSearchSkusPort();
        port.rows = List.of(row("SKU-001", "A"), row("SKU-002", "B"));
        port.total = 45;

        var result = service(port).execute("A", 2, 20);

        assertEquals(2, result.number());
        assertEquals(20, result.size());
        assertEquals(45, result.totalElements());
        assertFalse(result.last());
    }

    @Test
    @DisplayName("placements from port are attached to matching rows")
    void placements_attachedToRows() {
        Long skuA = 10L;
        Long skuB = 11L;
        var port = new FakeSearchSkusPort();
        port.rows = List.of(row(skuA, "SKU-A", "Shirt"), row(skuB, "SKU-B", "Pants"));
        port.total = 2;

        var placementPort = new FakePlacementPort(Map.of(
                skuA, List.of(new PlacementView("A-1-1", 30), new PlacementView(null, 10)),
                skuB, List.of(new PlacementView("B-2-2", 5))));

        var result = service(port, placementPort).execute("Shirt", 1, 20);

        assertEquals(2, result.content().size());

        SkuSearchRow rowA = result.content().stream().filter(r -> skuA.equals(r.skuId())).findFirst().orElseThrow();
        assertEquals(2, rowA.placements().size());
        assertEquals("A-1-1", rowA.placements().get(0).binLabel());
        assertEquals(30, rowA.placements().get(0).quantity());
        assertNull(rowA.placements().get(1).binLabel());

        SkuSearchRow rowB = result.content().stream().filter(r -> skuB.equals(r.skuId())).findFirst().orElseThrow();
        assertEquals(1, rowB.placements().size());
        assertEquals("B-2-2", rowB.placements().get(0).binLabel());
    }

    @Test
    @DisplayName("SKU with no placements gets an empty list (not null)")
    void skuWithNoPlacements_getsEmptyList() {
        Long skuId = 20L;
        var port = new FakeSearchSkusPort();
        port.rows = List.of(row(skuId, "SKU-X", "Widget"));
        port.total = 1;

        var result = service(port, NO_PLACEMENTS).execute("Widget", 1, 20);

        assertNotNull(result.content().get(0).placements());
        assertTrue(result.content().get(0).placements().isEmpty());
    }
}
