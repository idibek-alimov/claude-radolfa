package tj.radolfa.domain.exception;

public class DeliveryCodeAttemptsExhaustedException extends RuntimeException {
    public DeliveryCodeAttemptsExhaustedException(String message) {
        super(message);
    }
}
