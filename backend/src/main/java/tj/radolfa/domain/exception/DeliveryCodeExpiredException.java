package tj.radolfa.domain.exception;

public class DeliveryCodeExpiredException extends RuntimeException {
    public DeliveryCodeExpiredException(String message) {
        super(message);
    }
}
