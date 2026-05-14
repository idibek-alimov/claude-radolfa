package tj.radolfa.application.ports.in.review;

import tj.radolfa.domain.model.ReviewTraitInputType;

public interface UpdateReviewTraitUseCase {

    /**
     * Updates the label and input type of an existing trait.
     * The {@code key} is immutable — it is the API-stable identifier.
     *
     * @param id        trait ID
     * @param labelI18n new i18n key; must not be blank
     * @param inputType new input type; must not be null
     */
    void execute(Long id, String labelI18n, ReviewTraitInputType inputType);
}
