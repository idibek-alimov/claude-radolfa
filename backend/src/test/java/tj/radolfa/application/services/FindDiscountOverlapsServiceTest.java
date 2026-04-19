package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountOverlapRow;
import tj.radolfa.domain.model.DiscountTarget;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.domain.model.SkuTarget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FindDiscountOverlapsServiceTest {

    private FakeLoadDiscountPort fakePort;
    private FindDiscountOverlapsService service;

    private static final DiscountType FLASH  = new DiscountType(1L, "FLASH_SALE", 1);
    private static final DiscountType SEASON = new DiscountType(2L, "SEASONAL", 3);
    private static final Instant FROM = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant UPTO = Instant.parse("2099-12-31T00:00:00Z");

    @BeforeEach
    void setUp() {
        fakePort = new FakeLoadDiscountPort();
        service  = new FindDiscountOverlapsService(fakePort);
    }

    @Test
    @DisplayName("No active discounts → empty overlap list")
    void execute_noDiscounts_returnsEmpty() {
        fakePort.activeDiscounts = List.of();
        assertTrue(service.execute().isEmpty());
    }

    @Test
    @DisplayName("Each SKU covered by only one campaign → no overlaps")
    void execute_noSharedSkus_returnsEmpty() {
        fakePort.activeDiscounts = List.of(
                discount(1L, FLASH,  List.of("SKU-A")),
                discount(2L, SEASON, List.of("SKU-B"))
        );
        assertTrue(service.execute().isEmpty());
    }

    @Test
    @DisplayName("Two campaigns share one SKU → one overlap row, lower-rank campaign wins")
    void execute_twoOverlappingCampaigns_winnerHasLowerRank() {
        fakePort.activeDiscounts = List.of(
                discount(1L, FLASH,  List.of("SKU-A")),   // rank 1 — wins
                discount(2L, SEASON, List.of("SKU-A"))    // rank 3 — loses
        );

        List<DiscountOverlapRow> overlaps = service.execute();

        assertEquals(1, overlaps.size());
        DiscountOverlapRow row = overlaps.get(0);
        assertEquals("SKU-A", row.skuCode());
        assertEquals(1L, row.winningCampaign().id());
        assertEquals(1, row.losingCampaigns().size());
        assertEquals(2L, row.losingCampaigns().get(0).id());
    }

    @Test
    @DisplayName("Triple overlap on one SKU → one overlap row, one winner, two losers")
    void execute_tripleOverlap_oneWinnerTwoLosers() {
        fakePort.activeDiscounts = List.of(
                discount(10L, FLASH,  List.of("SKU-X")),
                discount(20L, SEASON, List.of("SKU-X")),
                discount(30L, SEASON, List.of("SKU-X"))   // same rank as 20, higher id → loses
        );

        List<DiscountOverlapRow> overlaps = service.execute();
        assertEquals(1, overlaps.size());

        DiscountOverlapRow row = overlaps.get(0);
        assertEquals(10L, row.winningCampaign().id());
        assertEquals(2, row.losingCampaigns().size());
    }

    @Test
    @DisplayName("Tie on rank → lower id wins")
    void execute_sameRank_lowerIdWins() {
        fakePort.activeDiscounts = List.of(
                discount(5L, SEASON,  List.of("SKU-Y")),   // rank 3, id 5 → wins (lower id)
                discount(9L, SEASON,  List.of("SKU-Y"))    // rank 3, id 9 → loses
        );

        List<DiscountOverlapRow> overlaps = service.execute();
        assertEquals(1, overlaps.size());
        assertEquals(5L, overlaps.get(0).winningCampaign().id());
    }

    @Test
    @DisplayName("Overlapping campaigns on different SKUs produce separate rows")
    void execute_overlapsOnDifferentSkus_separateRows() {
        fakePort.activeDiscounts = List.of(
                discount(1L, FLASH,  List.of("SKU-A", "SKU-B")),
                discount(2L, SEASON, List.of("SKU-A")),
                discount(3L, SEASON, List.of("SKU-B"))
        );

        List<DiscountOverlapRow> overlaps = service.execute();
        assertEquals(2, overlaps.size());
    }

    // ---- Helpers ----

    private static Discount discount(Long id, DiscountType type, List<String> codes) {
        List<DiscountTarget> targets = codes.stream().<DiscountTarget>map(SkuTarget::new).toList();
        return new Discount(id, type, targets, AmountType.PERCENT, new BigDecimal("10.00"),
                FROM, UPTO, false, "Camp-" + id, "#FFFFFF", null, null, null, null);
    }

    // ---- Fake ----

    static class FakeLoadDiscountPort implements LoadDiscountPort {
        List<Discount> activeDiscounts = new ArrayList<>();

        @Override
        public Optional<Discount> findById(Long id) {
            return activeDiscounts.stream().filter(d -> d.id().equals(id)).findFirst();
        }

        @Override
        public List<Discount> findActiveByItemCode(String itemCode) {
            return activeDiscounts.stream()
                    .filter(d -> d.itemCodes().contains(itemCode))
                    .toList();
        }

        @Override
        public List<Discount> findActiveByItemCodes(Collection<String> itemCodes) {
            return activeDiscounts.stream()
                    .filter(d -> d.itemCodes().stream().anyMatch(itemCodes::contains))
                    .toList();
        }

        @Override
        public Page<Discount> findAll(DiscountFilter filter, Pageable pageable) {
            return new PageImpl<>(activeDiscounts, pageable, activeDiscounts.size());
        }
    }
}
