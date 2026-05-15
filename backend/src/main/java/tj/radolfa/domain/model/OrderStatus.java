package tj.radolfa.domain.model;

public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERY_ATTEMPTED,
    READY_FOR_PICKUP,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
