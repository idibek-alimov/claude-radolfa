package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.DeleteReviewTraitUseCase;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.SaveReviewTraitPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

@Service
public class DeleteReviewTraitService implements DeleteReviewTraitUseCase {

    private final LoadReviewTraitPort loadReviewTraitPort;
    private final SaveReviewTraitPort saveReviewTraitPort;

    public DeleteReviewTraitService(LoadReviewTraitPort loadReviewTraitPort,
                                    SaveReviewTraitPort saveReviewTraitPort) {
        this.loadReviewTraitPort = loadReviewTraitPort;
        this.saveReviewTraitPort = saveReviewTraitPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        loadReviewTraitPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewTrait not found: id=" + id));
        saveReviewTraitPort.deleteById(id);
    }
}
