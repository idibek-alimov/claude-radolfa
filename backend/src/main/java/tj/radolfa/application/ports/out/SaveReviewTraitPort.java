package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ReviewTrait;

public interface SaveReviewTraitPort {

    ReviewTrait save(ReviewTrait trait);

    void deleteById(Long id);
}
