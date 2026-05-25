package tj.radolfa.domain.exception;

public class ReturnItemQuantityExceededException extends RuntimeException {
    public ReturnItemQuantityExceededException(String message) {
        super(message);
    }
}
