package tj.radolfa.application.ports.in.order;

public interface ConfirmRecallReceivedUseCase {
    /**
     * Called by COURIER (for courier-held recalls) or PICKPOINT_STAFF (for pickpoint recalls)
     * to confirm the physical package has been returned.
     * Transitions RECALL_REQUESTED → CANCELLED and restores stock.
     */
    void execute(Long orderId, Long actorUserId);
}
