package tj.radolfa.application.ports.in.payment;

/**
 * In-Port: issue a refund for a completed payment.
 *
 * <p>ADMIN only. Responsibilities:
 * <ol>
 *   <li>Call the payment gateway refund endpoint via {@code PaymentPort}.</li>
 *   <li>Transition the {@code Payment} to REFUNDED.</li>
 *   <li>Transition the linked {@code Order} to CANCELLED.</li>
 *   <li>Restore stock for all order items.</li>
 * </ol>
 */
public interface RefundPaymentUseCase {

    /**
     * @param orderId    the order whose payment to refund
     * @param adminUserId the ADMIN user requesting the refund (for audit)
     */
    void execute(Long orderId, Long adminUserId);
}
