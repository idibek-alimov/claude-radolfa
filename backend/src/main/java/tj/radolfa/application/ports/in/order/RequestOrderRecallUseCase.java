package tj.radolfa.application.ports.in.order;

public interface RequestOrderRecallUseCase {
    /**
     * ADMIN only. Initiates a physical recall for an order that is in transit or at a pickup point.
     * Transitions the order to RECALL_REQUESTED and notifies the assigned courier or pickpoint.
     */
    void execute(Long orderId, Long adminUserId, String reason);
}
