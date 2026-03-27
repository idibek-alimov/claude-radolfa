package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.payment.InitiatePaymentUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.PaymentPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Payment;

/**
 * Starts the payment flow for a PENDING order.
 *
 * <ol>
 *   <li>Verify the order is PENDING and owned by the requesting user.</li>
 *   <li>Call the payment gateway to get a redirect URL and transaction ID.</li>
 *   <li>Persist a {@code Payment} record in PENDING state.</li>
 *   <li>Return the gateway redirect URL so the client can forward the user.</li>
 * </ol>
 */
@Service
public class InitiatePaymentService implements InitiatePaymentUseCase {

    private static final String DEFAULT_CURRENCY = "TJS";

    private final LoadOrderPort   loadOrderPort;
    private final SavePaymentPort savePaymentPort;
    private final PaymentPort     paymentPort;

    public InitiatePaymentService(LoadOrderPort loadOrderPort,
                                  SavePaymentPort savePaymentPort,
                                  PaymentPort paymentPort) {
        this.loadOrderPort   = loadOrderPort;
        this.savePaymentPort = savePaymentPort;
        this.paymentPort     = paymentPort;
    }

    @Override
    @Transactional
    public Result execute(Long orderId, Long userId, String provider) {
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.userId().equals(userId)) {
            throw new IllegalStateException("Order does not belong to the requesting user");
        }
        if (order.status() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment can only be initiated for PENDING orders, current status: " + order.status());
        }

        // Call the payment gateway
        PaymentPort.PaymentIntent intent = paymentPort.initiate(
                order.totalAmount(),
                DEFAULT_CURRENCY,
                orderId.toString(),
                userId.toString()
        );

        // Persist payment record
        Payment payment = Payment.initiate(
                orderId,
                order.totalAmount(),
                DEFAULT_CURRENCY,
                provider,
                intent.redirectUrl()
        );
        // Attach the gateway transaction ID immediately (PROCESSING state)
        payment = payment.processing(intent.transactionId());

        Payment saved = savePaymentPort.save(payment);

        return new Result(saved.id(), intent.redirectUrl());
    }
}
