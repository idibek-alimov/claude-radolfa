package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.ReviewTraitInputType;

/**
 * Storefront-facing projection of a review trait.
 * Omits the database id — consumers identify traits by their immutable key.
 */
public record ReviewTraitView(
        String key,
        String labelI18n,
        ReviewTraitInputType inputType
) {}
