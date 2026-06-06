package tj.radolfa.infrastructure.persistence.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.application.readmodel.SkuDto;
import tj.radolfa.infrastructure.persistence.adapter.DiscountEnrichmentAdapter.DiscountInfo;
import tj.radolfa.infrastructure.persistence.entity.ProductRatingSummaryEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListingGridRowMapperTest {

    // Column layout: [0]=id, [1]=slug, [2]=name, [3]=categoryName, [4]=colorKey,
    // [5]=webDesc, [6]=MIN(originalPrice), [7]=totalStock,
    // [8]=colorHex, [9]=productCode, [10]=MAX(originalPrice), [11]=productBaseId
    private static Object[] row(Long variantId) {
        return new Object[]{
                variantId,               // [0] id
                "slug-" + variantId,     // [1] slug
                "Product Name",          // [2] name
                "Clothing",              // [3] categoryName
                "RED",                   // [4] colorKey
                "A description",         // [5] webDescription
                new BigDecimal("199.00"),// [6] MIN(originalPrice)
                10L,                     // [7] totalStock
                "#FF0000",               // [8] colorHex
                "RD-001",                // [9] productCode
                new BigDecimal("299.00"),// [10] MAX(originalPrice)
                100L                     // [11] productBaseId
        };
    }

    @Test
    @DisplayName("rating fields populated when ratingMap contains the variant")
    void ratingPresentInMap() {
        Long variantId = 42L;
        ProductRatingSummaryEntity summary = new ProductRatingSummaryEntity();
        summary.setListingVariantId(variantId);
        summary.setAverageRating(new BigDecimal("4.80"));
        summary.setReviewCount(412);

        ListingVariantDto dto = ListingGridRowMapper.toGridDto(
                row(variantId),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(variantId, summary));

        assertNotNull(dto.ratingAverage());
        assertEquals(0, new BigDecimal("4.80").compareTo(dto.ratingAverage()));
        assertEquals(412, dto.reviewCount());
    }

    @Test
    @DisplayName("ratingAverage is null and reviewCount is 0 when variant absent from ratingMap")
    void ratingAbsentFromMap() {
        Long variantId = 99L;

        ListingVariantDto dto = ListingGridRowMapper.toGridDto(
                row(variantId),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()); // empty ratingMap

        assertNull(dto.ratingAverage());
        assertEquals(0, dto.reviewCount());
    }

    @Test
    @DisplayName("toGridDto maps core row fields correctly regardless of rating")
    void coreFieldsMapping() {
        Long variantId = 7L;

        ListingVariantDto dto = ListingGridRowMapper.toGridDto(
                row(variantId),
                Map.of(variantId, List.of("https://cdn/img1.jpg")),
                Map.of(),
                Map.of(variantId, List.of(new SkuDto(1L, "RD-001-S", "S", 5,
                        new BigDecimal("199.00"), null, null, null, null, null))),
                Map.of(),
                Map.of());

        assertEquals(variantId, dto.variantId());
        assertEquals(100L, dto.productBaseId());
        assertEquals("slug-7", dto.slug());
        assertEquals("Product Name", dto.colorDisplayName());
        assertEquals("Clothing", dto.categoryName());
        assertEquals("RED", dto.colorKey());
        assertEquals("#FF0000", dto.colorHex());
        assertEquals(1, dto.images().size());
        assertEquals("https://cdn/img1.jpg", dto.images().get(0));
        assertEquals(1, dto.skus().size());
    }

    @Test
    @DisplayName("discount info overrides grid-query price when present in discountMap")
    void discountOverridesPrice() {
        Long variantId = 5L;
        DiscountInfo discountInfo = new DiscountInfo(
                new BigDecimal("200.00"),  // originalPrice
                new BigDecimal("150.00"),  // discountedPrice
                new BigDecimal("25"),      // discountPercentage
                Instant.now(),             // validUpto
                "Summer Sale",             // saleTitle
                "#FF0000",                 // saleColorHex
                "PERCENTAGE",             // typeName
                false);                    // isPartialDiscount

        ListingVariantDto dto = ListingGridRowMapper.toGridDto(
                row(variantId),
                Map.of(),
                Map.of(variantId, discountInfo),
                Map.of(),
                Map.of(),
                Map.of());

        assertEquals(new BigDecimal("200.00"), dto.originalPrice());
        assertEquals(new BigDecimal("150.00"), dto.discountPrice());
        assertEquals(25, dto.discountPercentage());
        assertEquals("Summer Sale", dto.discountName());
    }
}
