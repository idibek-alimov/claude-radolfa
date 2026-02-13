package tj.radolfa.infrastructure.web.dto;

import java.util.List;

/**
 * A single horizontal row on the homepage (e.g. "Featured", "New Arrivals").
 *
 * <p>The frontend loops {@code sections.map(s -> <Row ... />)} —
 * no hardcoded section logic needed client-side.
 */
public record HomeSectionDto(
        String key,
        String title,
        List<ListingVariantDto> items) {
}
