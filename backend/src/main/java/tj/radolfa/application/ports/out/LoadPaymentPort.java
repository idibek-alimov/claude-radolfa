package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Payment;

import java.util.Optional;

/**
 * Out-Port: load payment records from the persistence layer.
 */
public interface LoadPaymentPort {

    /**
     * Finds the most recent payment for an order.
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Finds a payment by the gateway's transaction ID.
     * Used when processing provider webhooks.
     */
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
}
