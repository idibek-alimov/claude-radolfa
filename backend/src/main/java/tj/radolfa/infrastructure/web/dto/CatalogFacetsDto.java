package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.CatalogFacets;
import tj.radolfa.application.readmodel.CategoryView;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web-layer facet payload for {@code GET /api/v1/listings/catalog}.
 *
 * <p>Wraps {@link CatalogFacets} (brands/colors/price/discount counts, computed
 * by the catalog read path) and adds {@code categories} — the static list of
 * child categories (or root categories when browsing without a category
 * context), resolved by the controller from {@code GetCategoryUseCase}
 * since {@link CatalogFacets} deliberately omits per-category counts.
 */
public record CatalogFacetsDto(
        List<CategoryFacetDto> categories,
        List<BrandFacetDto> brands,
        List<ColorFacetDto> colors,
        PriceFacetDto price,
        DiscountBucketsDto discountBuckets) {

    public record CategoryFacetDto(Long id, String name, String slug) {}

    public record BrandFacetDto(Long id, String name, long count) {}

    public record ColorFacetDto(String key, String name, String hex, long count) {}

    public record PriceFacetDto(BigDecimal min, BigDecimal max) {}

    public record DiscountBucketsDto(long any, long p30, long p50) {}

    public static CatalogFacetsDto from(CatalogFacets facets, List<CategoryView> categories) {
        return new CatalogFacetsDto(
                categories.stream()
                        .map(c -> new CategoryFacetDto(c.id(), c.name(), c.slug()))
                        .toList(),
                facets.brands().stream()
                        .map(b -> new BrandFacetDto(b.id(), b.name(), b.count()))
                        .toList(),
                facets.colors().stream()
                        .map(c -> new ColorFacetDto(c.key(), c.name(), c.hex(), c.count()))
                        .toList(),
                new PriceFacetDto(facets.price().min(), facets.price().max()),
                new DiscountBucketsDto(
                        facets.discounts().any(),
                        facets.discounts().p30(),
                        facets.discounts().p50()));
    }
}
