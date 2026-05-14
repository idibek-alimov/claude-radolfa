package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.review.ListReviewTraitsUseCase;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.domain.model.ReviewTrait;

import java.util.List;

@Service
public class ListReviewTraitsService implements ListReviewTraitsUseCase {

    private final LoadReviewTraitPort loadReviewTraitPort;

    public ListReviewTraitsService(LoadReviewTraitPort loadReviewTraitPort) {
        this.loadReviewTraitPort = loadReviewTraitPort;
    }

    @Override
    public List<ReviewTrait> execute() {
        return loadReviewTraitPort.findAll();
    }
}
