package tj.radolfa.domain.exception;

public class OrderItemAlreadyFullyPickedException extends RuntimeException {

    private final Long orderItemId;
    private final int quantity;

    public OrderItemAlreadyFullyPickedException(Long orderItemId, int quantity) {
        super("Order item " + orderItemId + " is already fully picked (quantity=" + quantity + ")");
        this.orderItemId = orderItemId;
        this.quantity = quantity;
    }

    public Long getOrderItemId() { return orderItemId; }
    public int getQuantity() { return quantity; }
}
