package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Facet counts for the catalog sidebar/sheet, computed alongside the page of
 * results in {@code GetListingUseCase#searchCatalog}.
 *
 * <p>Categories are deliberately absent — the controller builds the static
 * child-category list from {@code CategoryRepository} (no per-category counts,
 * see Phase 7b of the search redesign plan).
 */
public record CatalogFacets(
        List<BrandFacet> brands,
        List<ColorFacet> colors,
        PriceFacet price,
        DiscountFacets discounts) {

    /** No facet data — used by adapters that don't compute facets (SQL fallback in this phase). */
    public static CatalogFacets empty() {
        return new CatalogFacets(List.of(), List.of(), new PriceFacet(null, null), new DiscountFacets(0, 0, 0));
    }

    public record BrandFacet(Long id, String name, long count) {}

    public record ColorFacet(String key, String name, String hex, long count) {}

    public record PriceFacet(BigDecimal min, BigDecimal max) {}

    /**
     * Counts of active listings matching each discount threshold.
     *
     * @param any count with {@code discountPercentage > 0}
     * @param p30 count with {@code discountPercentage >= 30}
     * @param p50 count with {@code discountPercentage >= 50}
     */
    public record DiscountFacets(long any, long p30, long p50) {}
}
