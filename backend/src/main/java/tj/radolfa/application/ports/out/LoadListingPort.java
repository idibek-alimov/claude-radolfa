package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.web.dto.ListingVariantDetailDto;
import tj.radolfa.infrastructure.web.dto.ListingVariantDto;

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
     * SQL LIKE fallback search when Elasticsearch is unavailable.
     */
    PageResult<ListingVariantDto> search(String query, int page, int limit);
}
