package tj.radolfa.domain.exception;

public class DeliveryCodeAlreadyUsedException extends RuntimeException {
    public DeliveryCodeAlreadyUsedException(String message) {
        super(message);
    }
}
