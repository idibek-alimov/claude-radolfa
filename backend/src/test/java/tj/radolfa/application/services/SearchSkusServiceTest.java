package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.SearchSkusPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuSearchRow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchSkusServiceTest {

    static SkuSearchRow row(String code, String productName) {
        return new SkuSearchRow(1L, code, "BAR-" + code, "M", 10, productName, null);
    }

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

    SearchSkusService service(FakeSearchSkusPort port) {
        return new SearchSkusService(port);
    }

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
}
