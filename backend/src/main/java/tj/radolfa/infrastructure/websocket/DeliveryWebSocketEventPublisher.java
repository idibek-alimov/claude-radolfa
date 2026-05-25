package tj.radolfa.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.DeliveryEventPublisher;
import tj.radolfa.infrastructure.websocket.dto.DeliveryEvent;

@Component
public class DeliveryWebSocketEventPublisher implements DeliveryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWebSocketEventPublisher.class);

    private final SimpMessagingTemplate messaging;

    public DeliveryWebSocketEventPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void publishOrderCancelledToCourier(Long courierId, Long orderId) {
        send(() -> messaging.convertAndSendToUser(
                courierId.toString(), "/queue/delivery",
                DeliveryEvent.of("ORDER_CANCELLED", orderId)));
    }

    @Override
    public void publishOrderAssignedToCourier(Long courierId, Long orderId) {
        send(() -> messaging.convertAndSendToUser(
                courierId.toString(), "/queue/delivery",
                DeliveryEvent.of("ORDER_ASSIGNED", orderId)));
    }

    @Override
    public void publishNewOrderAtPickpoint(Long pickpointId, Long orderId) {
        send(() -> messaging.convertAndSend(
                "/topic/pickpoint/" + pickpointId,
                DeliveryEvent.of("ORDER_READY_FOR_PICKUP", orderId)));
    }

    @Override
    public void publishOrderCancelledAtPickpoint(Long pickpointId, Long orderId) {
        send(() -> messaging.convertAndSend(
                "/topic/pickpoint/" + pickpointId,
                DeliveryEvent.of("PICKPOINT_ORDER_CANCELLED", orderId)));
    }

    @Override
    public void publishDeliveryRetryLimitReached(Long orderId, Long courierId) {
        send(() -> messaging.convertAndSend(
                "/topic/admin/alerts",
                DeliveryEvent.retryLimit(orderId, courierId)));
    }

    @Override
    public void publishOrderRecallToCourier(Long courierId, Long orderId) {
        send(() -> messaging.convertAndSendToUser(
                courierId.toString(), "/queue/delivery",
                DeliveryEvent.of("ORDER_RECALL_REQUESTED", orderId)));
    }

    @Override
    public void publishOrderRecallToPickpoint(Long pickpointId, Long orderId) {
        send(() -> messaging.convertAndSend(
                "/topic/pickpoint/" + pickpointId,
                DeliveryEvent.of("ORDER_RECALL_REQUESTED", orderId)));
    }

    private void send(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("[WS-PUBLISH] Failed to deliver WebSocket event: {}", ex.getMessage());
        }
    }
}
