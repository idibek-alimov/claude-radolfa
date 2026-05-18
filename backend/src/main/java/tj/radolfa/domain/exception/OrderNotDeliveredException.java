package tj.radolfa.domain.exception;

public class OrderNotDeliveredException extends RuntimeException {
    public OrderNotDeliveredException(String message) {
        super(message);
    }
}
