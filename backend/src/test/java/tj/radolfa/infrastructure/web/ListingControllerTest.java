package tj.radolfa.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tj.radolfa.application.ports.in.GetListingUseCase;
import tj.radolfa.application.ports.in.UpdateListingUseCase;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDetailDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;
import tj.radolfa.infrastructure.web.dto.SkuDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc test for {@link ListingController}.
 *
 * <p>
 * Verifies endpoint mapping, JSON structure, and 404 handling.
 * Uses an in-memory fake (no Spring context, no security filter chain).
 */
class ListingControllerTest {

    private MockMvc mockMvc;
    private FakeGetListingUseCase fakeUseCase;

    @BeforeEach
    void setUp() {
        fakeUseCase = new FakeGetListingUseCase();
        ListingController controller = new ListingController(fakeUseCase, new FakeUpdateListingUseCase());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/listings returns paginated grid")
    void grid_returnsPaginatedResult() throws Exception {
        mockMvc.perform(get("/api/v1/listings")
                .param("page", "1")
                .param("limit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].slug").value("tpl-001-red"))
                .andExpect(jsonPath("$.items[0].name").value("T-Shirt"))
                .andExpect(jsonPath("$.items[0].priceStart").value(24.99))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/listings/{slug} returns detail with SKUs and siblings")
    void detail_returnsFullDetail() throws Exception {
        mockMvc.perform(get("/api/v1/listings/tpl-001-red"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("tpl-001-red"))
                .andExpect(jsonPath("$.skus").isArray())
                .andExpect(jsonPath("$.skus[0].sizeLabel").value("S"))
                .andExpect(jsonPath("$.skus[0].onSale").value(true))
                .andExpect(jsonPath("$.siblingVariants").isArray())
                .andExpect(jsonPath("$.siblingVariants[0].slug").value("tpl-001-blue"))
                .andExpect(jsonPath("$.siblingVariants[0].colorKey").value("blue"));
    }

    @Test
    @DisplayName("GET /api/v1/listings/{slug} returns 404 for unknown slug")
    void detail_returns404ForUnknownSlug() throws Exception {
        mockMvc.perform(get("/api/v1/listings/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/listings/search returns search results")
    void search_returnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/listings/search")
                .param("q", "shirt")
                .param("page", "1")
                .param("limit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].slug").value("tpl-001-red"));
    }

    @Test
    @DisplayName("GET /api/v1/listings/autocomplete returns suggestions")
    void autocomplete_returnsSuggestions() throws Exception {
        mockMvc.perform(get("/api/v1/listings/autocomplete")
                .param("q", "shi")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("T-Shirt"))
                .andExpect(jsonPath("$[1]").value("Shirt Dress"));
    }

    // ==== Fake ====

    static class FakeGetListingUseCase implements GetListingUseCase {

        private static final ListingVariantDto GRID_ITEM = new ListingVariantDto(
                1L, "tpl-001-red", "T-Shirt", "red",
                "A red cotton tee",
                List.of("https://s3/img1.jpg"),
                new BigDecimal("24.99"), new BigDecimal("29.99"), 50,
                false);

        @Override
        public PageResult<ListingVariantDto> getPage(int page, int limit) {
            return new PageResult<>(List.of(GRID_ITEM), 1, page, false);
        }

        @Override
        public Optional<ListingVariantDetailDto> getBySlug(String slug) {
            if (!"tpl-001-red".equals(slug))
                return Optional.empty();

            return Optional.of(new ListingVariantDetailDto(
                    1L, "tpl-001-red", "T-Shirt", "red",
                    "A red cotton tee",
                    List.of("https://s3/img1.jpg"),
                    new BigDecimal("24.99"), new BigDecimal("29.99"), 50,
                    List.of(new SkuDto(
                            100L, "TPL-001-RED-S", "S", 50,
                            new BigDecimal("29.99"), new BigDecimal("24.99"),
                            true, null)),
                    List.of(new ListingVariantDetailDto.SiblingVariant(
                            "tpl-001-blue", "blue", "https://s3/blue-thumb.jpg"))));
        }

        @Override
        public PageResult<ListingVariantDto> search(String query, int page, int limit) {
            return new PageResult<>(List.of(GRID_ITEM), 1, page, false);
        }

        @Override
        public List<String> autocomplete(String prefix, int limit) {
            return List.of("T-Shirt", "Shirt Dress");
        }
    }

    static class FakeUpdateListingUseCase implements UpdateListingUseCase {
        @Override
        public void update(String slug, UpdateListingCommand command) {
        }

        @Override
        public void addImage(String slug, String imageUrl) {
        }

        @Override
        public void removeImage(String slug, String imageUrl) {
        }
    }
}
