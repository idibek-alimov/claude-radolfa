package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.CreateReviewTraitUseCase;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.SaveReviewTraitPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;

@Service
public class CreateReviewTraitService implements CreateReviewTraitUseCase {

    private final LoadReviewTraitPort loadReviewTraitPort;
    private final SaveReviewTraitPort saveReviewTraitPort;

    public CreateReviewTraitService(LoadReviewTraitPort loadReviewTraitPort,
                                    SaveReviewTraitPort saveReviewTraitPort) {
        this.loadReviewTraitPort = loadReviewTraitPort;
        this.saveReviewTraitPort = saveReviewTraitPort;
    }

    @Override
    @Transactional
    public Long execute(String key, String labelI18n, ReviewTraitInputType inputType) {
        // Domain constructor validates key format, labelI18n non-blank, inputType non-null
        ReviewTrait trait = new ReviewTrait(null, key, labelI18n, inputType, null, null);

        if (loadReviewTraitPort.existsByKey(key)) {
            throw new DuplicateResourceException("A review trait with key '" + key + "' already exists");
        }

        ReviewTrait saved = saveReviewTraitPort.save(trait);
        return saved.getId();
    }
}
