package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.PageResult;

/**
 * Paginated response for a single collection's "View All" page.
 * Includes the display title so the frontend doesn't need to maintain a key-to-title mapping.
 */
public record CollectionPageDto(
        String key,
        String title,
        PageResult<ListingVariantDto> page) {
}
