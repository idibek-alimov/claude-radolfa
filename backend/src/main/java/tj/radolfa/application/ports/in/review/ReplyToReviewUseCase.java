package tj.radolfa.application.ports.in.review;

public interface ReplyToReviewUseCase {

    void execute(Long reviewId, String replyText);
}
