package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tj.radolfa.application.ports.out.LoadListingPort;
import tj.radolfa.application.ports.out.SearchListingPort;
import tj.radolfa.application.readmodel.CatalogFacets;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingSort;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GetListingService#searchCatalog}.
 *
 * <p>Uses in-memory fakes — no Spring context, no Mockito — following the
 * convention established by {@link UpdateListingServiceTest}.
 */
class GetListingServiceTest {

    private FakeSearchListingPort fakeSearch;
    private FakeLoadListingPort fakeLoad;
    private GetListingService service;

    @BeforeEach
    void setUp() {
        fakeSearch = new FakeSearchListingPort();
        fakeLoad = new FakeLoadListingPort();
        service = new GetListingService(fakeLoad, fakeSearch);
    }

    private static ListingVariantDto sampleDto(Long variantId) {
        return new ListingVariantDto(
                1L, variantId, "slug-" + variantId, "Red", "Bags",
                "red", "#FF0000", "desc", List.of(),
                null, null, null, null, null,
                null, null, null, false,
                List.of(), "RD-10047", List.of(),
                null, 0, null,
                null, null);
    }

    // =========================================================
    //  searchCatalog()
    // =========================================================

    @Test
    @DisplayName("searchCatalog() delegates to Elasticsearch and passes the criteria through unchanged")
    void searchCatalog_delegatesToElasticsearch() {
        ListingQueryCriteria criteria = new ListingQueryCriteria(
                "leather bag", List.of(5L), null, null, null, List.of(), List.of(), true, ListingSort.CHEAPEST);
        fakeSearch.result = new CatalogResult(
                new PageResult<>(List.of(sampleDto(1L)), 1, 1, 12, true), CatalogFacets.empty());

        CatalogResult result = service.searchCatalog(criteria, 1, 12);

        assertSame(fakeSearch.result, result);
        assertSame(criteria, fakeSearch.lastCriteria);
        assertNull(fakeLoad.lastCriteria, "SQL fallback must not be invoked when ES succeeds");
    }

    @Test
    @DisplayName("searchCatalog() falls back to SQL when Elasticsearch throws, passing the same criteria")
    void searchCatalog_esThrows_fallsBackToSql() {
        ListingQueryCriteria criteria = new ListingQueryCriteria(
                "bag", List.of(), null, null, null, List.of(), List.of(), null, ListingSort.NEWEST);
        fakeSearch.throwOnSearchCatalog = true;
        fakeLoad.result = new CatalogResult(
                new PageResult<>(List.of(sampleDto(2L)), 1, 1, 12, true), CatalogFacets.empty());

        CatalogResult result = service.searchCatalog(criteria, 1, 12);

        assertSame(fakeLoad.result, result);
        assertSame(criteria, fakeSearch.lastCriteria);
        assertSame(criteria, fakeLoad.lastCriteria);
    }

    @Test
    @DisplayName("searchCatalog() with a product-code query bypasses Elasticsearch entirely")
    void searchCatalog_productCodeQuery_bypassesElasticsearch() {
        ListingQueryCriteria criteria = new ListingQueryCriteria(
                "rd-10047", List.of(), null, null, null, List.of(), List.of(), null, ListingSort.POPULAR);
        fakeLoad.findByProductCodeResult = new PageResult<>(List.of(sampleDto(3L)), 1, 1, 12, true);

        CatalogResult result = service.searchCatalog(criteria, 1, 12);

        assertSame(fakeLoad.findByProductCodeResult, result.page());
        assertEquals("RD-10047", fakeLoad.lastProductCode);
        assertNull(fakeSearch.lastCriteria, "Elasticsearch must not be queried for an exact product code");
        assertNull(fakeLoad.lastCriteria, "SQL searchCatalog must not be invoked for an exact product code");
    }

    @Test
    @DisplayName("searchCatalog() clamps limit to MAX_PAGE_SIZE before reaching the ports")
    void searchCatalog_clampsLimitToMaxPageSize() {
        ListingQueryCriteria criteria = ListingQueryCriteria.empty();

        service.searchCatalog(criteria, 1, 500);

        assertEquals(100, fakeSearch.lastLimit);
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    static class FakeSearchListingPort implements SearchListingPort {
        ListingQueryCriteria lastCriteria;
        int lastLimit;
        boolean throwOnSearchCatalog;
        CatalogResult result = new CatalogResult(new PageResult<>(List.of(), 0, 1, 12, true), CatalogFacets.empty());

        @Override
        public PageResult<ListingVariantDto> search(String query, int page, int limit) {
            return new PageResult<>(List.of(), 0, page, limit, true);
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
            return List.of();
        }

        @Override
        public CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit) {
            lastCriteria = criteria;
            lastLimit = limit;
            if (throwOnSearchCatalog) {
                throw new RuntimeException("Elasticsearch unavailable");
            }
            return result;
        }
    }

    static class FakeLoadListingPort implements LoadListingPort {
        ListingQueryCriteria lastCriteria;
        String lastProductCode;
        CatalogResult result = new CatalogResult(new PageResult<>(List.of(), 0, 1, 12, true), CatalogFacets.empty());
        PageResult<ListingVariantDto> findByProductCodeResult = new PageResult<>(List.of(), 0, 1, 12, true);

        @Override
        public PageResult<ListingVariantDto> loadPage(int page, int limit) {
            return new PageResult<>(List.of(), 0, page, limit, true);
        }

        @Override
        public Optional<ListingVariantDetailDto> loadBySlug(String slug) {
            return Optional.empty();
        }

        @Override
        public PageResult<ListingVariantDto> search(String query, int page, int limit) {
            return new PageResult<>(List.of(), 0, page, limit, true);
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
            return List.of();
        }

        @Override
        public PageResult<ListingVariantDto> loadByCategoryIds(List<Long> categoryIds, int page, int limit) {
            return new PageResult<>(List.of(), 0, page, limit, true);
        }

        @Override
        public PageResult<ListingVariantDto> findByProductCode(String code, int page, int limit) {
            lastProductCode = code;
            return findByProductCodeResult;
        }

        @Override
        public CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit) {
            lastCriteria = criteria;
            return result;
        }
    }
}
