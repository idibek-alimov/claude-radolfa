package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.ListingVariantDto;
import tj.radolfa.infrastructure.web.PageResponse;

/**
 * Response body for {@code GET /api/v1/listings/catalog} — the requested page
 * of listings plus facet counts for the filter sidebar/sheet, in one round-trip.
 */
public record CatalogResponseDto(PageResponse<ListingVariantDto> page, CatalogFacetsDto facets) {}
