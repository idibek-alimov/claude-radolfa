package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadSkuPriceHistoryPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.SkuPriceChange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetSkuPriceHistoryServiceTest {

    private static SkuPriceChange change(Long id) {
        return new SkuPriceChange(id, 100L, "SKU-001",
                new BigDecimal("29.99"), new BigDecimal("49.99"),
                1L, "ADMIN_PANEL", Instant.now());
    }

    // ── Sort-whitelist guard ─────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown sortBy falls back to occurredAt")
    void unknownSortBy_fallsBackToOccurredAt() {
        String[] capturedSortBy = new String[1];
        LoadSkuPriceHistoryPort spy = (skuId, sortBy, sortDir, page, size) -> {
            capturedSortBy[0] = sortBy;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        GetSkuPriceHistoryService service = new GetSkuPriceHistoryService(spy);
        service.execute(100L, "DROP TABLE", "DESC", 1, 20);

        assertEquals("occurredAt", capturedSortBy[0]);
    }

    @Test
    @DisplayName("Valid sortBy (newPrice) is forwarded as-is")
    void validSortBy_forwarded() {
        String[] capturedSortBy = new String[1];
        LoadSkuPriceHistoryPort spy = (skuId, sortBy, sortDir, page, size) -> {
            capturedSortBy[0] = sortBy;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        GetSkuPriceHistoryService service = new GetSkuPriceHistoryService(spy);
        service.execute(100L, "newPrice", "ASC", 1, 20);

        assertEquals("newPrice", capturedSortBy[0]);
    }

    @Test
    @DisplayName("sortDir defaults to DESC when unknown value is passed")
    void unknownSortDir_defaultsToDesc() {
        String[] capturedDir = new String[1];
        LoadSkuPriceHistoryPort spy = (skuId, sortBy, sortDir, page, size) -> {
            capturedDir[0] = sortDir;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        GetSkuPriceHistoryService service = new GetSkuPriceHistoryService(spy);
        service.execute(100L, "occurredAt", "RANDOM", 1, 20);

        assertEquals("DESC", capturedDir[0]);
    }

    // ── Size cap ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Size cap enforced at 100")
    void sizeCap_enforcedAt100() {
        int[] capturedSize = new int[1];
        LoadSkuPriceHistoryPort spy = (skuId, sortBy, sortDir, page, size) -> {
            capturedSize[0] = size;
            return new PageResult<>(List.of(), 0, page, size, true);
        };

        GetSkuPriceHistoryService service = new GetSkuPriceHistoryService(spy);
        service.execute(100L, "occurredAt", "DESC", 1, 9999);

        assertEquals(100, capturedSize[0]);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Result is passed through from port unchanged")
    void resultPassedThrough() {
        List<SkuPriceChange> rows = List.of(change(1L), change(2L));
        LoadSkuPriceHistoryPort port = (skuId, sortBy, sortDir, page, size) ->
                new PageResult<>(rows, 2, page, size, true);

        GetSkuPriceHistoryService service = new GetSkuPriceHistoryService(port);
        PageResult<SkuPriceChange> result =
                service.execute(100L, "occurredAt", "DESC", 1, 20);

        assertEquals(2, result.totalElements());
        assertEquals(2, result.content().size());
        assertEquals(1L, result.content().get(0).id());
    }
}
