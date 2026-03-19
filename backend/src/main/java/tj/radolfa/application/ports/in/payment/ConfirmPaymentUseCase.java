package tj.radolfa.application.ports.in.payment;

/**
 * In-Port: handle a successful payment callback from the provider.
 *
 * <p>Called by the payment webhook controller. Responsibilities:
 * <ol>
 *   <li>Validate idempotency — skip if already processed.</li>
 *   <li>Transition the {@code Payment} to COMPLETED.</li>
 *   <li>Transition the linked {@code Order} to PAID.</li>
 *   <li>Trigger loyalty points award.</li>
 * </ol>
 */
public interface ConfirmPaymentUseCase {

    /**
     * @param providerTransactionId the transaction ID as reported by the provider
     * @param webhookPayload        raw JSON payload from the provider (stored for audit)
     */
    void execute(String providerTransactionId, String webhookPayload);
}
