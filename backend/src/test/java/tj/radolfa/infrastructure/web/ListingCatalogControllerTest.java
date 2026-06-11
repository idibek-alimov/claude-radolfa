package tj.radolfa.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tj.radolfa.application.ports.in.GetCategoryUseCase;
import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.application.ports.in.ResolveUserDiscountUseCase;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.application.ports.in.UploadImageUseCase;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadRatingSummaryPort;
import tj.radolfa.application.readmodel.CatalogFacets;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.CategoryView;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingSort;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.PageResult;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc test for {@code GET /api/v1/listings/catalog}
 * (Phase 4 of the search redesign).
 *
 * <p>No Spring context, no Mockito — hand-written in-memory fakes for every
 * port, matching the project convention.
 */
class ListingCatalogControllerTest {

    private MockMvc mockMvc;
    private FakeGetListingUseCase fakeListingUseCase;
    private FakeGetCategoryUseCase fakeCategoryUseCase;

    @BeforeEach
    void setUp() {
        fakeListingUseCase = new FakeGetListingUseCase();
        fakeCategoryUseCase = new FakeGetCategoryUseCase();
        TierPricingEnricher tierPricing = new TierPricingEnricher(userId -> BigDecimal.ZERO);

        ListingController controller = new ListingController(
                fakeListingUseCase,
                fakeCategoryUseCase,
                new FakeUpdateListingUseCase(),
                new FakeUploadImageUseCase(),
                tierPricing,
                new FakeLoadListingVariantPort(),
                new FakeLoadRatingSummaryPort());

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /catalog returns the page and facets, passing sort through unchanged")
    void catalog_returnsPageAndFacets() throws Exception {
        mockMvc.perform(get("/api/v1/listings/catalog")
                        .param("q", "leather")
                        .param("sort", "CHEAPEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.content[0].slug").value("bag-red"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.facets.brands[0].name").value("Acme"))
                .andExpect(jsonPath("$.facets.colors[0].key").value("red"))
                .andExpect(jsonPath("$.facets.price.min").value(100))
                .andExpect(jsonPath("$.facets.discountBuckets.p30").value(2));

        assertEquals(ListingSort.CHEAPEST, fakeListingUseCase.lastCriteria.sort());
    }

    @Test
    @DisplayName("GET /catalog?sort=GARBAGE silently defaults to POPULAR")
    void catalog_unknownSort_defaultsToPopular() throws Exception {
        mockMvc.perform(get("/api/v1/listings/catalog").param("sort", "GARBAGE"))
                .andExpect(status().isOk());

        assertEquals(ListingSort.POPULAR, fakeListingUseCase.lastCriteria.sort());
    }

    @Test
    @DisplayName("GET /catalog?categorySlug=bags resolves descendant IDs and the child-category facet")
    void catalog_categorySlug_resolvesDescendantsAndCategoryFacet() throws Exception {
        mockMvc.perform(get("/api/v1/listings/catalog").param("categorySlug", "bags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facets.categories[0].slug").value("handbags"))
                .andExpect(jsonPath("$.facets.categories[1].slug").value("backpacks"));

        assertEquals(List.of(10L, 11L), fakeListingUseCase.lastCriteria.categoryIds());
    }

    @Test
    @DisplayName("GET /catalog without a category falls back to root categories for the category facet")
    void catalog_noCategory_usesRootCategoriesForFacet() throws Exception {
        mockMvc.perform(get("/api/v1/listings/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facets.categories[0].slug").value("bags"))
                .andExpect(jsonPath("$.facets.categories[1].slug").value("shoes"));
    }

    @Test
    @DisplayName("GET /catalog?categorySlug=does-not-exist returns 404")
    void catalog_unknownCategorySlug_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/listings/catalog").param("categorySlug", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    //  In-memory fakes
    // =========================================================

    private static ListingVariantDto sampleDto() {
        return new ListingVariantDto(
                1L, 1L, "bag-red", "Red", "Bags",
                "red", "#FF0000", "desc", List.of(),
                new BigDecimal("199.00"), null, null, null, null,
                null, null, null, false,
                List.of(), "RD-10047", List.of(),
                null, 0, null,
                1L, "Acme");
    }

    private static CatalogResult sampleResult() {
        CatalogFacets facets = new CatalogFacets(
                List.of(new CatalogFacets.BrandFacet(1L, "Acme", 5)),
                List.of(new CatalogFacets.ColorFacet("red", "Red", "#FF0000", 3)),
                new CatalogFacets.PriceFacet(new BigDecimal("100"), new BigDecimal("500")),
                new CatalogFacets.DiscountFacets(4, 2, 1));
        return new CatalogResult(new PageResult<>(List.of(sampleDto()), 1, 1, 24, true), facets);
    }

    static class FakeGetListingUseCase implements GetListingUseCase {
        ListingQueryCriteria lastCriteria;

        @Override
        public PageResult<ListingVariantDto> getPage(int page, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ListingVariantDetailDto> getBySlug(String slug) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<ListingVariantDto> search(String query, int page, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<ListingVariantDto> getByCategoryIds(List<Long> categoryIds, int page, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit) {
            lastCriteria = criteria;
            return sampleResult();
        }
    }

    /**
     * Category tree:
     * <pre>
     * 10 Bags (root)
     *   11 Handbags
     *   12 Backpacks
     * 20 Shoes (root)
     * </pre>
     */
    static class FakeGetCategoryUseCase implements GetCategoryUseCase {
        private static final List<CategoryView> ALL = List.of(
                new CategoryView(10L, "Bags", "bags", null, List.of()),
                new CategoryView(11L, "Handbags", "handbags", 10L, List.of()),
                new CategoryView(12L, "Backpacks", "backpacks", 10L, List.of()),
                new CategoryView(20L, "Shoes", "shoes", null, List.of()));

        @Override
        public List<CategoryView> findAll() {
            return ALL;
        }

        @Override
        public Optional<CategoryView> findBySlug(String slug) {
            return ALL.stream().filter(c -> c.slug().equals(slug)).findFirst();
        }

        @Override
        public List<Long> getDescendantIds(Long categoryId) {
            if (categoryId.equals(10L)) {
                return List.of(10L, 11L);
            }
            return List.of(categoryId);
        }

        @Override
        public Optional<CategoryView> findById(Long id) {
            return ALL.stream().filter(c -> c.id().equals(id)).findFirst();
        }
    }

    static class FakeUpdateListingUseCase implements UpdateListingUseCase {
        @Override
        public void update(String slug, UpdateListingCommand command) {
        }

        @Override
        public void updateDimensions(String slug, UpdateDimensionsCommand command) {
        }

        @Override
        public void addImage(String slug, String imageUrl) {
        }

        @Override
        public void removeImage(String slug, String imageUrl) {
        }
    }

    static class FakeUploadImageUseCase implements UploadImageUseCase {
        @Override
        public String upload(String slug, InputStream imageStream, String originalFilename) {
            throw new UnsupportedOperationException();
        }
    }

    static class FakeLoadListingVariantPort implements LoadListingVariantPort {
        @Override
        public Optional<ListingVariant> findVariantById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<ListingVariant> findByProductBaseIdAndColorKey(Long productBaseId, String colorKey) {
            return Optional.empty();
        }

        @Override
        public Optional<ListingVariant> findBySlug(String slug) {
            return Optional.empty();
        }

        @Override
        public List<ListingVariant> findAllByProductBaseId(Long productBaseId) {
            return List.of();
        }

        @Override
        public Map<Long, ListingVariant> findVariantsByIds(Collection<Long> ids) {
            return Map.of();
        }
    }

    static class FakeLoadRatingSummaryPort implements LoadRatingSummaryPort {
        @Override
        public Optional<RatingSummaryView> findByVariantId(Long listingVariantId) {
            return Optional.empty();
        }
    }
}
