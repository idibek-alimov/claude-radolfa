package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Review;

public interface SaveReviewPort {

    Review save(Review review);
}
