package tj.radolfa.domain.exception;

public class OrderAlreadyClaimedException extends RuntimeException {
    public OrderAlreadyClaimedException(String message) {
        super(message);
    }
}
