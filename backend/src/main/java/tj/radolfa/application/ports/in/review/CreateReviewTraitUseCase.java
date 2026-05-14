package tj.radolfa.application.ports.in.review;

import tj.radolfa.domain.model.ReviewTraitInputType;

public interface CreateReviewTraitUseCase {

    /**
     * @param key        API-stable identifier (lowercase letters, digits, underscores; max 64 chars)
     * @param labelI18n  i18n key for the frontend label
     * @param inputType  how the trait is collected in the review form
     * @return ID of the newly created trait
     */
    Long execute(String key, String labelI18n, ReviewTraitInputType inputType);
}
