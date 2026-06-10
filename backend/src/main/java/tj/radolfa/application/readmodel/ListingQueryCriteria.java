package tj.radolfa.application.readmodel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application-layer filter/sort criteria for the unified catalog read path
 * ({@code GetListingUseCase#searchCatalog}).
 *
 * <p>Carried verbatim through {@code SearchListingPort} (Elasticsearch) and
 * {@code LoadListingPort} (SQL fallback) — neither port nor adapter receives
 * raw strings for filters. {@code sort} is always a whitelisted
 * {@link ListingSort}; never interpolate user-supplied sort fields into a
 * query.
 *
 * @param query              free-text search term; {@code null}/blank = browse (no text query).
 * @param categoryIds        category + descendant IDs to restrict to; empty = all categories.
 * @param priceMin           inclusive lower bound on price; {@code null} = no lower bound.
 * @param priceMax           inclusive upper bound on price; {@code null} = no upper bound.
 * @param minDiscountPercent minimum {@code discountPercentage}; {@code null}/0 = no discount filter.
 * @param colorKeys          colour keys to restrict to; empty = all colours.
 * @param brandIds           brand IDs to restrict to; empty = all brands.
 * @param inStockOnly        when {@code true}, only variants with stock > 0.
 * @param sort               whitelisted sort order; never {@code null} (defaults to {@link ListingSort#POPULAR}).
 */
public record ListingQueryCriteria(
        String query,
        List<Long> categoryIds,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Integer minDiscountPercent,
        List<String> colorKeys,
        List<Long> brandIds,
        Boolean inStockOnly,
        ListingSort sort) {

    /** Normalizes nullable lists/sort so callers and adapters never see {@code null} for them. */
    public ListingQueryCriteria {
        categoryIds = categoryIds != null ? categoryIds : List.of();
        colorKeys = colorKeys != null ? colorKeys : List.of();
        brandIds = brandIds != null ? brandIds : List.of();
        sort = sort != null ? sort : ListingSort.POPULAR;
    }

    /** No filters, no query, default sort — equivalent to "browse everything". */
    public static ListingQueryCriteria empty() {
        return new ListingQueryCriteria(null, List.of(), null, null, null, List.of(), List.of(), null, ListingSort.POPULAR);
    }

    /** True when a non-blank free-text query was supplied. */
    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

    /** True when the result set should be restricted to one or more categories. */
    public boolean hasCategoryFilter() {
        return !categoryIds.isEmpty();
    }
}
