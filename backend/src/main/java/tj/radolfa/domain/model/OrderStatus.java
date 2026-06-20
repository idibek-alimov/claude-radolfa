package tj.radolfa.domain.model;

public enum OrderStatus {
    PENDING,
    PAID,
    AWAITING_COD,
    PICKED,
    CLAIMED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERY_ATTEMPTED,
    RECALL_REQUESTED,
    READY_FOR_PICKUP,
    RETURN_INITIATED,
    RETURNED_TO_WAREHOUSE,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean isPickable() {
        return this == PAID || this == AWAITING_COD;
    }
}
