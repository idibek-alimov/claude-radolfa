package tj.radolfa.application.ports.in.review;

public interface ModerateReviewUseCase {

    void approve(Long reviewId);

    void reject(Long reviewId);
}
