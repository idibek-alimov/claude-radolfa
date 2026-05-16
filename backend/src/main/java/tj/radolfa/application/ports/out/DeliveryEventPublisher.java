package tj.radolfa.application.ports.out;

public interface DeliveryEventPublisher {

    /** Notifies the assigned courier that their order was cancelled. */
    void publishOrderCancelledToCourier(Long courierId, Long orderId);

    /** Notifies a courier that a new order has been assigned to them (SHIPPED). */
    void publishOrderAssignedToCourier(Long courierId, Long orderId);

    /** Notifies pickpoint staff that a new order is READY_FOR_PICKUP at their location. */
    void publishNewOrderAtPickpoint(Long pickpointId, Long orderId);

    /** Notifies pickpoint staff that a waiting order has been cancelled. */
    void publishOrderCancelledAtPickpoint(Long pickpointId, Long orderId);

    /** Pushes a retry-limit-reached alert to the admin panel. */
    void publishDeliveryRetryLimitReached(Long orderId, Long courierId);
}
