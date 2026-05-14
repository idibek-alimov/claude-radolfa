package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Payment;

/**
 * Out-Port: persist a payment record (insert or update).
 */
public interface SavePaymentPort {

    /**
     * Saves the payment and returns the persisted instance (with generated ID if new).
     */
    Payment save(Payment payment);
}
