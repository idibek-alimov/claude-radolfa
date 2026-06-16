package tj.radolfa.application.readmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link ListingVariantDetailDto#withLoyalty} preserves the display-only
 * fields added in Phase 1 (ratingAverage, reviewCount, colorName, categorySlug) while
 * correctly stamping loyalty pricing on variant and SKU.
 */
class ListingVariantDetailDtoTest {

    private static final BigDecimal ORIGINAL_PRICE = BigDecimal.valueOf(100_00, 2);

    private ListingVariantDetailDto buildDto() {
        SkuDto sku = new SkuDto(
                1L, "SKU-001", "M", 10,
                ORIGINAL_PRICE, null, null, null, null, null, null);

        return new ListingVariantDetailDto(
                1L, 2L, "cotton-tshirt-black", "Essential Cotton T-Shirt", "Tops",
                "midnight-black", "#1A1A2E", "Great tshirt", List.of(),
                List.of(),
                ORIGINAL_PRICE, null, null, null, null,
                null, null, null, false,
                List.of(), List.of(sku), List.of(), "10001",
                0.3, 30, 40, 2,
                List.of(),
                null, null,
                BigDecimal.valueOf(4_50, 2), 10, "Midnight Black", "tops");
    }

    @Test
    @DisplayName("withLoyalty preserves ratingAverage, reviewCount, colorName, categorySlug")
    void withLoyalty_preservesPhase1Fields() {
        ListingVariantDetailDto original = buildDto();

        ListingVariantDetailDto enriched = original.withLoyalty(BigDecimal.valueOf(15));

        assertEquals(new BigDecimal("4.50"), enriched.ratingAverage());
        assertEquals(10, enriched.reviewCount());
        assertEquals("Midnight Black", enriched.colorName());
        assertEquals("tops", enriched.categorySlug());
    }

    @Test
    @DisplayName("withLoyalty stamps loyaltyPrice on the variant")
    void withLoyalty_stampsLoyaltyPrice() {
        ListingVariantDetailDto enriched = buildDto().withLoyalty(BigDecimal.valueOf(15));

        // 100.00 * (1 - 0.15) = 85.00
        assertEquals(new BigDecimal("85.00"), enriched.loyaltyPrice());
        assertEquals(15, enriched.loyaltyPercentage());
    }
}
