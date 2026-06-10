package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.PageResult;

/**
 * Combined result of {@code GetListingUseCase#searchCatalog}: the page of
 * listings plus facet counts for the sidebar/sheet, computed in a single
 * round-trip by the search adapter.
 */
public record CatalogResult(PageResult<ListingVariantDto> page, CatalogFacets facets) {}
