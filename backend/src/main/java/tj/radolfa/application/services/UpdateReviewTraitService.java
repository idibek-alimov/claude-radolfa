package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.UpdateReviewTraitUseCase;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.SaveReviewTraitPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.domain.model.ReviewTraitInputType;

@Service
public class UpdateReviewTraitService implements UpdateReviewTraitUseCase {

    private final LoadReviewTraitPort loadReviewTraitPort;
    private final SaveReviewTraitPort saveReviewTraitPort;

    public UpdateReviewTraitService(LoadReviewTraitPort loadReviewTraitPort,
                                    SaveReviewTraitPort saveReviewTraitPort) {
        this.loadReviewTraitPort = loadReviewTraitPort;
        this.saveReviewTraitPort = saveReviewTraitPort;
    }

    @Override
    @Transactional
    public void execute(Long id, String labelI18n, ReviewTraitInputType inputType) {
        ReviewTrait trait = loadReviewTraitPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewTrait not found: id=" + id));

        // Domain methods validate non-blank / non-null and update timestamps
        trait.updateLabel(labelI18n);
        trait.updateInputType(inputType);

        saveReviewTraitPort.save(trait);
    }
}
