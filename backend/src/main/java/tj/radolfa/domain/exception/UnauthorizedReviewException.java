package tj.radolfa.domain.exception;

public class UnauthorizedReviewException extends RuntimeException {
    public UnauthorizedReviewException(String message) {
        super(message);
    }
}
