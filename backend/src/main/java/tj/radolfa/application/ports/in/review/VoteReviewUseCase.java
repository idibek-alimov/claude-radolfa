package tj.radolfa.application.ports.in.review;

import tj.radolfa.domain.model.VoteType;

public interface VoteReviewUseCase {

    void execute(Command command);

    record Command(Long reviewId, Long userId, VoteType vote) {}
}
