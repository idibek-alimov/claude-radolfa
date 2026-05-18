package tj.radolfa.domain.exception;

public class OrderNotAtPickpointException extends RuntimeException {
    public OrderNotAtPickpointException(String message) {
        super(message);
    }
}
