package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.discount.GetTopCampaignsUseCase;
import tj.radolfa.application.ports.out.QueryDiscountMetricsPort;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetTopCampaignsServiceTest {

    // ---- Fake ----

    static class FakeQueryDiscountMetricsPort implements QueryDiscountMetricsPort {
        String capturedBy;
        LocalDate capturedFrom;
        LocalDate capturedTo;
        int capturedLimit;
        List<TopCampaignRow> stubResult = List.of();

        @Override
        public DiscountMetrics findMetrics(Long discountId, LocalDate from, LocalDate to) {
            return new DiscountMetrics(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, from, to, List.of());
        }

        @Override
        public List<TopCampaignRow> findTop(String by, LocalDate from, LocalDate to, int limit) {
            capturedBy    = by;
            capturedFrom  = from;
            capturedTo    = to;
            capturedLimit = limit;
            return stubResult;
        }
    }

    // ---- Setup ----

    private FakeQueryDiscountMetricsPort fakePort;
    private GetTopCampaignsService service;

    @BeforeEach
    void setUp() {
        fakePort = new FakeQueryDiscountMetricsPort();
        service  = new GetTopCampaignsService(fakePort);
    }

    // ---- Tests ----

    @Test
    @DisplayName("execute: defaults → by=revenue, period=30d, limit=5")
    void execute_defaults_revenueAnd30d() {
        service.execute(new GetTopCampaignsUseCase.Command(null, null, 0));

        assertEquals("revenue", fakePort.capturedBy);
        assertEquals(5, fakePort.capturedLimit);
        assertEquals(30, daysBetween(fakePort.capturedFrom, fakePort.capturedTo));
    }

    @Test
    @DisplayName("execute: by=units, period=7d respected")
    void execute_unitsAnd7d() {
        service.execute(new GetTopCampaignsUseCase.Command("units", "7d", 5));

        assertEquals("units", fakePort.capturedBy);
        assertEquals(7, daysBetween(fakePort.capturedFrom, fakePort.capturedTo));
    }

    @Test
    @DisplayName("execute: period=90d → 90-day window")
    void execute_90dPeriod() {
        service.execute(new GetTopCampaignsUseCase.Command("revenue", "90d", 5));

        assertEquals(90, daysBetween(fakePort.capturedFrom, fakePort.capturedTo));
    }

    @Test
    @DisplayName("execute: invalid by value defaults to revenue")
    void execute_invalidBy_defaultsToRevenue() {
        service.execute(new GetTopCampaignsUseCase.Command("invalid", "30d", 5));

        assertEquals("revenue", fakePort.capturedBy);
    }

    @Test
    @DisplayName("execute: invalid period defaults to 30d")
    void execute_invalidPeriod_defaultsTo30d() {
        service.execute(new GetTopCampaignsUseCase.Command("revenue", "999d", 5));

        assertEquals(30, daysBetween(fakePort.capturedFrom, fakePort.capturedTo));
    }

    @Test
    @DisplayName("execute: empty result returns empty list")
    void execute_emptyResult() {
        List<TopCampaignRow> result = service.execute(new GetTopCampaignsUseCase.Command("revenue", "30d", 5));
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("execute: port result is passed through")
    void execute_resultPassthrough() {
        DiscountSummary summary = new DiscountSummary(
                1L, "Flash", "#FFF", new BigDecimal("15"), AmountType.PERCENT, new DiscountType(1L, "Flash", 1));
        fakePort.stubResult = List.of(new TopCampaignRow(summary, 3L, 10L, new BigDecimal("150.00")));

        List<TopCampaignRow> result = service.execute(new GetTopCampaignsUseCase.Command("revenue", "30d", 5));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).campaign().id());
    }

    private long daysBetween(LocalDate from, LocalDate to) {
        return java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }
}
