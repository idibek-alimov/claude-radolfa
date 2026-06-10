package tj.radolfa.application.readmodel;

/**
 * Whitelisted sort options for {@link ListingQueryCriteria}-driven catalog
 * queries (search results, category listings).
 *
 * <p>This enum is the <b>only</b> sort vocabulary accepted from the outside
 * world. Adapters (Elasticsearch in {@code ListingSearchAdapter}, SQL
 * {@code Specification} in {@code ListingReadAdapter}) map each constant to
 * a concrete, safe sort field/direction — raw client input must never reach
 * a JPQL/native {@code ORDER BY} clause.
 */
public enum ListingSort {

    /**
     * Default. With a non-blank {@link ListingQueryCriteria#query()}, ranks by
     * Elasticsearch relevance ({@code _score}). When browsing without a query,
     * falls back to the existing default ordering (variant id ascending) until
     * a units-sold-based ranking is wired in a later phase.
     */
    POPULAR,

    /** Ascending by effective price ({@code price asc}). */
    CHEAPEST,

    /** Descending by effective price ({@code price desc}). */
    DEAREST,

    /** Descending by {@code ratingAverage} (highest rated first). */
    RATING,

    /** Descending by {@code createdAt} (most recently added first). */
    NEWEST,

    /** Descending by {@code discountPercentage} (deepest discount first). */
    BIGGEST_DISCOUNT;

    /**
     * Resolves a sort name from client input, defaulting to {@link #POPULAR}
     * for {@code null}, blank, or unrecognised values. Used by the catalog
     * controller so an invalid {@code sort} query param never causes a 400 —
     * it silently falls back to the default ordering.
     */
    public static ListingSort fromNullable(String name) {
        if (name == null || name.isBlank()) {
            return POPULAR;
        }
        try {
            return ListingSort.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return POPULAR;
        }
    }
}
