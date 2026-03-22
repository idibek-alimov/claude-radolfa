package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.PageResult;
import tj.radolfa.application.readmodel.ListingVariantDetailDto;
import tj.radolfa.application.readmodel.ListingVariantDto;

import java.util.List;
import java.util.Optional;


/**
 * In-Port: storefront read operations for listing variants.
 */
public interface GetListingUseCase {

    /**
     * Paginated grid of colour cards.
     */
    PageResult<ListingVariantDto> getPage(int page, int limit);

    /**
     * Detail page: variant + SKUs + sibling colour swatches.
     */
    Optional<ListingVariantDetailDto> getBySlug(String slug);

    /**
     * Full-text search (ES with SQL fallback).
     */
    PageResult<ListingVariantDto> search(String query, int page, int limit);

    /**
     * Autocomplete suggestions for the search box.
     */
    List<String> autocomplete(String prefix, int limit);

    /**
     * Paginated grid filtered by a set of category IDs (category + descendants).
     */
    PageResult<ListingVariantDto> getByCategoryIds(List<Long> categoryIds, int page, int limit);
}
