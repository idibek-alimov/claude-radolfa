package tj.radolfa.infrastructure.websocket.dto;

public record DeliveryEvent(String type, Long orderId, Long courierId, Long pickpointId) {

    public static DeliveryEvent of(String type, Long orderId) {
        return new DeliveryEvent(type, orderId, null, null);
    }

    public static DeliveryEvent retryLimit(Long orderId, Long courierId) {
        return new DeliveryEvent("DELIVERY_RETRY_LIMIT_REACHED", orderId, courierId, null);
    }
}
