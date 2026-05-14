package tj.radolfa.application.ports.in.review;

import tj.radolfa.domain.model.ReviewTrait;

import java.util.List;

public interface ListReviewTraitsUseCase {

    List<ReviewTrait> execute();
}
