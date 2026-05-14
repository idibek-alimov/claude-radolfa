package tj.radolfa.application.ports.in.order;

/**
 * In-Port: cancel an order and restore reserved stock.
 *
 * <p>Rules:
 * <ul>
 *   <li>USER may only cancel their own PENDING orders.</li>
 *   <li>ADMIN may cancel any order that is not yet DELIVERED.</li>
 *   <li>Cancellation increments stock back via {@code UpdateProductStockUseCase}.</li>
 * </ul>
 */
public interface CancelOrderUseCase {

    /**
     * @param orderId     the order to cancel
     * @param requesterId the user ID requesting cancellation (for ownership check)
     * @param reason      optional cancellation reason
     */
    void execute(Long orderId, Long requesterId, String reason);
}
