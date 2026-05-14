package tj.radolfa.application.ports.in.payment;

/**
 * In-Port: start the payment flow for a PENDING order.
 *
 * <p>Creates a {@code Payment} record in PENDING state, calls the payment
 * gateway via {@code PaymentPort}, and returns the redirect URL so the
 * client can send the user to the provider's checkout page.
 */
public interface InitiatePaymentUseCase {

    /**
     * @param orderId  the order to pay
     * @param userId   must be the order owner
     * @param provider the gateway to use (e.g. "PAYME", "CLICK", "STUB")
     * @return payment details including the provider redirect URL
     */
    Result execute(Long orderId, Long userId, String provider);

    record Result(
            Long   paymentId,
            String redirectUrl
    ) {}
}
