package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadProductCardPort;
import tj.radolfa.application.readmodel.ProductCardDto;
import tj.radolfa.application.readmodel.ProductCardDto.AttributeDto;
import tj.radolfa.application.readmodel.ProductCardDto.ImageRef;
import tj.radolfa.application.readmodel.ProductCardDto.SkuSummary;
import tj.radolfa.application.readmodel.ProductCardDto.TagView;
import tj.radolfa.application.readmodel.ProductCardDto.VariantSummary;
import tj.radolfa.domain.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GetProductCardService}.
 *
 * <p>No Spring context, no Mockito — hand-written in-memory fake adapter.
 */
class GetProductCardServiceTest {

    private FakeLoadProductCardPort fakePort;
    private GetProductCardService service;

    @BeforeEach
    void setUp() {
        fakePort = new FakeLoadProductCardPort();
        service  = new GetProductCardService(fakePort);
    }

    // =========================================================
    //  Happy-path
    // =========================================================

    @Test
    @DisplayName("Returns full product card when product exists — 2 variants × 2 SKUs each")
    void execute_returnsCard_whenFound() {
        ProductCardDto card = buildCard(1L, "Winter Jacket", 2);
        fakePort.store(card);

        ProductCardDto result = service.execute(1L);

        assertEquals(1L, result.productBaseId());
        assertEquals("Winter Jacket", result.name());
        assertEquals(2, result.variants().size());

        VariantSummary first = result.variants().get(0);
        assertEquals(2, first.skus().size());
        assertEquals("red", first.colorKey());
        assertEquals(List.of(new ImageRef(1L, "https://cdn.example.com/img1.webp")), first.images());

        VariantSummary second = result.variants().get(1);
        assertEquals("blue", second.colorKey());
        assertEquals(1, second.attributes().size());
        assertEquals("Material", second.attributes().get(0).key());
        assertEquals(List.of("Cotton"), second.attributes().get(0).values());
    }

    @Test
    @DisplayName("Returns card with brand and category populated")
    void execute_returnsBrandAndCategory_whenSet() {
        ProductCardDto card = new ProductCardDto(
                10L, "Sneakers", "Nike", 3L, "Footwear", List.of());
        fakePort.store(card);

        ProductCardDto result = service.execute(10L);

        assertEquals("Nike", result.brand());
        assertEquals("Footwear", result.categoryName());
        assertEquals(3L, result.categoryId());
    }

    @Test
    @DisplayName("Returns card with null brand when no brand is assigned")
    void execute_returnsNullBrand_whenBrandAbsent() {
        ProductCardDto card = new ProductCardDto(
                20L, "Generic Shirt", null, 5L, "Tops", List.of());
        fakePort.store(card);

        ProductCardDto result = service.execute(20L);

        assertNull(result.brand());
    }

    // =========================================================
    //  Error path
    // =========================================================

    @Test
    @DisplayName("Throws ResourceNotFoundException when product does not exist")
    void execute_throwsResourceNotFoundException_whenMissing() {
        // nothing stored for id=99
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.execute(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    // =========================================================
    //  Fake
    // =========================================================

    static class FakeLoadProductCardPort implements LoadProductCardPort {
        private final Map<Long, ProductCardDto> store = new HashMap<>();

        void store(ProductCardDto card) {
            store.put(card.productBaseId(), card);
        }

        @Override
        public Optional<ProductCardDto> loadByProductBaseId(Long productBaseId) {
            return Optional.ofNullable(store.get(productBaseId));
        }
    }

    // =========================================================
    //  Builder helpers
    // =========================================================

    private ProductCardDto buildCard(Long baseId, String name, int variantCount) {
        List<String> colorKeys = List.of("red", "blue", "green", "yellow", "black");
        List<VariantSummary> variants = java.util.stream.IntStream.range(0, variantCount)
                .mapToObj(i -> buildVariant((long) (i + 1), colorKeys.get(i)))
                .toList();
        return new ProductCardDto(baseId, name, "BrandX", 1L, "Clothing", variants);
    }

    private VariantSummary buildVariant(Long variantId, String colorKey) {
        List<SkuSummary> skus = List.of(
                new SkuSummary(variantId * 10, "SKU-" + colorKey + "-S", "S", 10, new BigDecimal("49.99")),
                new SkuSummary(variantId * 10 + 1, "SKU-" + colorKey + "-M", "M", 5, new BigDecimal("49.99"))
        );
        List<AttributeDto> attrs = List.of(new AttributeDto("Material", List.of("Cotton")));
        List<TagView> tags = List.of(new TagView(1L, "Sale", "#FF0000"));

        return new VariantSummary(
                variantId,
                "winter-jacket-" + colorKey,
                "RD-0000" + variantId,
                variantId,
                colorKey,
                colorKey.substring(0, 1).toUpperCase() + colorKey.substring(1),
                "#AABBCC",
                "A great jacket in " + colorKey,
                List.of(new ImageRef(1L, "https://cdn.example.com/img1.webp")),
                attrs,
                tags,
                skus,
                true,
                true,
                0.8,
                30,
                50,
                10
        );
    }
}
