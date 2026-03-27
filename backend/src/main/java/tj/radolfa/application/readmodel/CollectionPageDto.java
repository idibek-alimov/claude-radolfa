package tj.radolfa.application.readmodel;

import java.util.List;

/**
 * Response for a single collection's "View All" page.
 * Includes the display title so the frontend doesn't need to maintain a key-to-title mapping.
 */
public record CollectionPageDto(
        String key,
        String title,
        List<ListingVariantDto> listings) {
}
