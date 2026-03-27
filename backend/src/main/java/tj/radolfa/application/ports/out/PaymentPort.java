package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Money;

import java.time.Instant;

/**
 * Out-Port: external payment gateway integration.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code PaymentPortStub} — dev/test; always succeeds instantly.</li>
 *   <li>{@code PaymePaymentAdapter} — production Payme gateway (Phase 8).</li>
 * </ul>
 */
public interface PaymentPort {

    /**
     * Initiates a charge at the payment gateway.
     *
     * @param amount           the amount to charge
     * @param currency         ISO 4217 code, e.g. "TJS"
     * @param externalOrderId  the order identifier sent to the gateway for correlation
     * @param customerId       the customer identifier at the gateway side
     * @return a {@link PaymentIntent} with the redirect URL and gateway transaction ID
     */
    PaymentIntent initiate(Money amount, String currency, String externalOrderId, String customerId);

    /**
     * Issues a full refund for a previously completed transaction.
     *
     * @param providerTransactionId the ID returned during {@link #initiate}
     * @param amount                the amount to refund
     * @return a {@link RefundResult} confirming the refund outcome
     */
    RefundResult refund(String providerTransactionId, Money amount);

    // ── Value objects ──────────────────────────────────────────────────────────

    record PaymentIntent(
            String  transactionId,   // gateway's reference for this transaction
            String  redirectUrl,     // URL to send the user to for payment
            Instant expiresAt        // when the payment link expires
    ) {}

    record RefundResult(
            String  refundId,
            boolean success,
            String  message
    ) {}
}
