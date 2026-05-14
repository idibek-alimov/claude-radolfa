package tj.radolfa.application.ports.in.order;

/**
 * In-Port: mark an order as REFUNDED (admin audit marker).
 *
 * <p>Rules:
 * <ul>
 *   <li>ADMIN only.</li>
 *   <li>Allowed source statuses: DELIVERED or CANCELLED.</li>
 *   <li>No stock or loyalty side effects — those were handled at cancel/delivery time.</li>
 * </ul>
 */
public interface RefundOrderUseCase {

    /**
     * @param orderId     the order to mark as refunded
     * @param requesterId the admin user ID initiating the refund
     * @param reason      optional reason (logged only — not persisted)
     */
    void execute(Long orderId, Long requesterId, String reason);
}
