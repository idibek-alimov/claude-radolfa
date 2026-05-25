package tj.radolfa.domain.exception;

import tj.radolfa.domain.model.OrderStatus;

public class OrderRecallNotAllowedException extends RuntimeException {
    public OrderRecallNotAllowedException(OrderStatus status) {
        super("Order cannot be recalled from status: " + status +
              ". Recall is only allowed for SHIPPED, OUT_FOR_DELIVERY, or READY_FOR_PICKUP orders.");
    }
}
