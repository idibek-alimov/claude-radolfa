package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.QueryDiscountMetricsPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetDiscountMetricsServiceTest {

    // ---- Fakes ----

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        private final Map<Long, Discount> store = new HashMap<>();

        void add(Discount d) { store.put(d.id(), d); }

        @Override public Optional<Discount> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Discount> findActiveByItemCode(String itemCode) { return List.of(); }
        @Override public List<Discount> findActiveByItemCodes(Collection<String> itemCodes) { return List.of(); }
        @Override public List<Discount> findActiveWithAnyNonSkuTarget() { return List.of(); }
        @Override public org.springframework.data.domain.Page<Discount> findAll(
                tj.radolfa.application.ports.out.DiscountFilter f,
                org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
    }

    static class FakeQueryDiscountMetricsPort implements QueryDiscountMetricsPort {
        LocalDate capturedFrom;
        LocalDate capturedTo;
        DiscountMetrics stubResult;

        @Override
        public DiscountMetrics findMetrics(Long discountId, LocalDate from, LocalDate to) {
            capturedFrom = from;
            capturedTo   = to;
            return stubResult != null ? stubResult : new DiscountMetrics(
                    5L, 10L, new BigDecimal("250.00"), new BigDecimal("50.00"),
                    from, to, List.of());
        }

        @Override public List<TopCampaignRow> findTop(String by, LocalDate from, LocalDate to, int limit) { return List.of(); }
    }

    // ---- Fixtures ----

    private static final LocalDate ANALYTICS_START = LocalDate.of(2026, 3, 1);
    private static final DiscountType TYPE = new DiscountType(1L, "Flash", 1, tj.radolfa.domain.model.StackingPolicy.BEST_WINS);

    private FakeLoadDiscountPort fakeLoad;
    private FakeQueryDiscountMetricsPort fakeQuery;
    private GetDiscountMetricsService service;

    @BeforeEach
    void setUp() {
        fakeLoad  = new FakeLoadDiscountPort();
        fakeQuery = new FakeQueryDiscountMetricsPort();
        service   = new GetDiscountMetricsService(fakeLoad, fakeQuery, ANALYTICS_START);
    }

    private Discount discount(Long id, LocalDate from, LocalDate to) {
        return new Discount(id, TYPE, List.of(new SkuTarget("SKU-1")), AmountType.PERCENT, new BigDecimal("15"),
                from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                to.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                false, "Summer Sale", "#FF0000", null, null, null, null);
    }

    // ---- Tests ----

    @Test
    @DisplayName("execute: campaign not found throws ResourceNotFoundException")
    void execute_notFound_throws() {
        assertThrows(ResourceNotFoundException.class, () -> service.execute(99L));
    }

    @Test
    @DisplayName("execute: campaign predates analytics start → returns zero metrics")
    void execute_predatesAnalytics_returnsZeroMetrics() {
        fakeLoad.add(discount(1L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));

        DiscountMetrics result = service.execute(1L);

        assertEquals(0L, result.ordersUsing());
        assertEquals(0L, result.unitsMoved());
        assertEquals(BigDecimal.ZERO, result.revenueUplift());
        assertTrue(result.dailySeries().isEmpty());
    }

    @Test
    @DisplayName("execute: happy path delegates correct window to port")
    void execute_happyPath_delegatesCorrectWindow() {
        LocalDate campaignFrom = LocalDate.of(2026, 3, 15);  // after analytics start, before today
        LocalDate campaignTo   = LocalDate.of(2026, 5, 25);  // after today → windowEnd = today
        fakeLoad.add(discount(2L, campaignFrom, campaignTo));

        service.execute(2L);

        assertEquals(campaignFrom, fakeQuery.capturedFrom);
        assertNotNull(fakeQuery.capturedTo);
        assertTrue(!fakeQuery.capturedTo.isAfter(campaignTo));
    }

    @Test
    @DisplayName("execute: analytics start clamps window start when campaign began before cutoff")
    void execute_campaignStartsBeforeCutoff_clampsToAnalyticsStart() {
        LocalDate campaignFrom = LocalDate.of(2026, 1, 1);
        LocalDate campaignTo   = LocalDate.of(2026, 12, 31);
        fakeLoad.add(discount(3L, campaignFrom, campaignTo));

        service.execute(3L);

        assertEquals(ANALYTICS_START, fakeQuery.capturedFrom);
    }

    @Test
    @DisplayName("execute: campaign end in the past clamps window end to campaign end")
    void execute_campaignEndedInPast_clampsWindowEnd() {
        LocalDate campaignFrom = ANALYTICS_START;
        LocalDate campaignTo   = ANALYTICS_START.plusDays(5);
        fakeLoad.add(discount(4L, campaignFrom, campaignTo));

        service.execute(4L);

        assertEquals(campaignTo, fakeQuery.capturedTo);
    }
}
