package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadHomeCollectionsPort;
import tj.radolfa.application.readmodel.CollectionPageDto;
import tj.radolfa.application.readmodel.HomeSectionDto;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetHomeCollectionsServiceTest {

    private FakeLoadHomeCollectionsPort fake;
    private GetHomeCollectionsService service;

    @BeforeEach
    void setUp() {
        fake = new FakeLoadHomeCollectionsPort();
        service = new GetHomeCollectionsService(fake);
    }

    // ---- getHomeSections ----

    @Test
    @DisplayName("top_sellers section included when loadTopSellers returns items")
    void topSellersIncludedWhenNonEmpty() {
        fake.topSellers = List.of(variant(1L), variant(2L));

        List<HomeSectionDto> sections = service.getHomeSections();

        assertTrue(sections.stream().anyMatch(s -> "top_sellers".equals(s.key())),
                "top_sellers section should be present");
        HomeSectionDto topSellers = sections.stream()
                .filter(s -> "top_sellers".equals(s.key()))
                .findFirst().orElseThrow();
        assertEquals("Top Sellers", topSellers.title());
        assertEquals(2, topSellers.listings().size());
    }

    @Test
    @DisplayName("top_sellers section omitted when loadTopSellers returns empty")
    void topSellersOmittedWhenEmpty() {
        fake.topSellers = List.of();

        List<HomeSectionDto> sections = service.getHomeSections();

        assertTrue(sections.stream().noneMatch(s -> "top_sellers".equals(s.key())),
                "top_sellers section should be absent when empty");
    }

    @Test
    @DisplayName("top_sellers section appears before new_arrivals in ordering")
    void topSellersBeforeNewArrivals() {
        fake.topSellers = List.of(variant(1L));
        fake.newArrivals = List.of(variant(2L));

        List<HomeSectionDto> sections = service.getHomeSections();

        List<String> keys = sections.stream().map(HomeSectionDto::key).toList();
        int topIdx = keys.indexOf("top_sellers");
        int newIdx = keys.indexOf("new_arrivals");
        assertTrue(topIdx >= 0, "top_sellers must be present");
        assertTrue(newIdx >= 0, "new_arrivals must be present");
        assertTrue(topIdx < newIdx, "top_sellers must appear before new_arrivals");
    }

    @Test
    @DisplayName("empty sections are omitted regardless of key")
    void emptySectionsOmitted() {
        // all sections empty by default
        List<HomeSectionDto> sections = service.getHomeSections();
        assertTrue(sections.isEmpty());
    }

    // ---- getSection ----

    @Test
    @DisplayName("getSection(top_sellers) returns CollectionPageDto with correct key and title")
    void getSectionTopSellers() {
        List<ListingVariantDto> items = List.of(variant(10L), variant(11L));
        fake.topSellersPage = new PageResult<>(items, 2, 1, 20, true);

        Optional<CollectionPageDto> result = service.getSection("top_sellers", 1, 20);

        assertTrue(result.isPresent());
        assertEquals("top_sellers", result.get().key());
        assertEquals("Top Sellers", result.get().title());
        assertEquals(2, result.get().listings().size());
    }

    @Test
    @DisplayName("getSection returns empty for unknown key")
    void getSectionUnknownKey() {
        Optional<CollectionPageDto> result = service.getSection("does_not_exist", 1, 20);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getSection(featured) still works after adding top_sellers")
    void getSectionFeaturedUnaffected() {
        List<ListingVariantDto> items = List.of(variant(5L));
        fake.featuredPage = new PageResult<>(items, 1, 1, 20, true);

        Optional<CollectionPageDto> result = service.getSection("featured", 1, 20);

        assertTrue(result.isPresent());
        assertEquals("featured", result.get().key());
        assertEquals("Featured", result.get().title());
    }

    // ---- Helpers ----

    private static ListingVariantDto variant(Long variantId) {
        return new ListingVariantDto(
                1L, variantId, "slug-" + variantId, null, null,
                null, null, null, List.of(),
                null, null, null, null, null,
                null, null, false, List.of(),
                null, List.of(),
                null, 0);
    }

    // ---- Fake out-port ----

    static class FakeLoadHomeCollectionsPort implements LoadHomeCollectionsPort {

        List<ListingVariantDto> featured = List.of();
        List<ListingVariantDto> newArrivals = List.of();
        List<ListingVariantDto> onSale = List.of();
        List<ListingVariantDto> topSellers = List.of();

        PageResult<ListingVariantDto> featuredPage = emptyPage(1, 20);
        PageResult<ListingVariantDto> newArrivalsPage = emptyPage(1, 20);
        PageResult<ListingVariantDto> onSalePage = emptyPage(1, 20);
        PageResult<ListingVariantDto> topSellersPage = emptyPage(1, 20);

        @Override public List<ListingVariantDto> loadFeatured(int limit) { return featured; }
        @Override public List<ListingVariantDto> loadNewArrivals(int limit) { return newArrivals; }
        @Override public List<ListingVariantDto> loadOnSale(int limit) { return onSale; }
        @Override public List<ListingVariantDto> loadTopSellers(int limit) { return topSellers; }

        @Override public PageResult<ListingVariantDto> loadFeaturedPage(int page, int limit) { return featuredPage; }
        @Override public PageResult<ListingVariantDto> loadNewArrivalsPage(int page, int limit) { return newArrivalsPage; }
        @Override public PageResult<ListingVariantDto> loadOnSalePage(int page, int limit) { return onSalePage; }
        @Override public PageResult<ListingVariantDto> loadTopSellersPage(int page, int limit) { return topSellersPage; }

        private static PageResult<ListingVariantDto> emptyPage(int page, int limit) {
            return new PageResult<>(List.of(), 0, page, limit, true);
        }
    }
}
