package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.application.readmodel.CatalogResult;
import tj.radolfa.application.readmodel.ListingQueryCriteria;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDto;

import java.util.List;
import java.util.Optional;

/**
 * Out-Port: SQL-backed read queries for listing variants.
 *
 * <p>Used for the grid (paginated with aggregates), detail page
 * (slug lookup with SKUs and siblings), and as a fallback when
 * Elasticsearch is unavailable.
 */
public interface LoadListingPort {

    /**
     * Paginated grid of listing variants with aggregated price/stock.
     */
    PageResult<ListingVariantDto> loadPage(int page, int limit);

    /**
     * Full detail for a single variant: SKUs + sibling colour swatches.
     */
    Optional<ListingVariantDetailDto> loadBySlug(String slug);

    /**
     * Full detail resolved by product code (article number, e.g. "10052").
     */
    Optional<ListingVariantDetailDto> loadByProductCode(String code);

    /**
     * SQL LIKE fallback search when Elasticsearch is unavailable.
     */
    PageResult<ListingVariantDto> search(String query, int page, int limit);

    /**
     * SQL LIKE fallback autocomplete when Elasticsearch is unavailable.
     */
    List<String> autocomplete(String prefix, int limit);

    /**
     * Paginated grid filtered by a set of category IDs (category + descendants).
     */
    PageResult<ListingVariantDto> loadByCategoryIds(List<Long> categoryIds, int page, int limit);

    /**
     * Exact product-code lookup (e.g. "10047").
     * Returns a single-item page when the code exists, empty page otherwise.
     */
    PageResult<ListingVariantDto> findByProductCode(String code, int page, int limit);

    /**
     * Unified catalog query: full-text + structured filters + whitelisted sort.
     * SQL fallback for {@link tj.radolfa.application.ports.out.SearchListingPort#searchCatalog},
     * used when Elasticsearch is unavailable.
     *
     * <p>Filters/sort are applied via a JPA {@code Specification}
     * ({@code tj.radolfa.infrastructure.persistence.spec.ListingSpecifications}); facets
     * (brand/colour/price) are computed by SQL group-by queries over the same filtered set.
     * Discount-threshold filtering and {@code BIGGEST_DISCOUNT} sort run at reduced
     * fidelity (see {@code ListingSpecifications} Javadoc) — discount-bucket facet counts
     * are {@code 0} on this path.
     */
    CatalogResult searchCatalog(ListingQueryCriteria criteria, int page, int limit);
}
