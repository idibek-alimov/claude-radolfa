package tj.radolfa.domain.exception;

public class RefundFailedException extends RuntimeException {
    public RefundFailedException(String reason) {
        super(reason);
    }
}
