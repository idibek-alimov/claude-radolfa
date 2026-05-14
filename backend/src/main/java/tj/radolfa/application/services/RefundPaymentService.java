package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.RevokeAwardedPointsUseCase;
import tj.radolfa.application.ports.in.order.CancelOrderUseCase;
import tj.radolfa.application.ports.in.payment.RefundPaymentUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.PaymentPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.domain.model.PaymentStatus;

/**
 * Issues a refund for a completed payment (ADMIN only).
 *
 * <ol>
 *   <li>Load the payment for the given order.</li>
 *   <li>Verify it is in a refundable state (COMPLETED).</li>
 *   <li>Call the payment gateway refund endpoint.</li>
 *   <li>Transition the {@code Payment} to REFUNDED and persist.</li>
 *   <li>Cancel the order and restore stock via {@link CancelOrderUseCase}.</li>
 * </ol>
 */
@Service
public class RefundPaymentService implements RefundPaymentUseCase {

    private final LoadPaymentPort            loadPaymentPort;
    private final SavePaymentPort            savePaymentPort;
    private final PaymentPort               paymentPort;
    private final LoadOrderPort             loadOrderPort;
    private final CancelOrderUseCase        cancelOrderUseCase;
    private final RevokeAwardedPointsUseCase revokeAwardedPointsUseCase;

    public RefundPaymentService(LoadPaymentPort loadPaymentPort,
                                SavePaymentPort savePaymentPort,
                                PaymentPort paymentPort,
                                LoadOrderPort loadOrderPort,
                                CancelOrderUseCase cancelOrderUseCase,
                                RevokeAwardedPointsUseCase revokeAwardedPointsUseCase) {
        this.loadPaymentPort            = loadPaymentPort;
        this.savePaymentPort            = savePaymentPort;
        this.paymentPort               = paymentPort;
        this.loadOrderPort             = loadOrderPort;
        this.cancelOrderUseCase        = cancelOrderUseCase;
        this.revokeAwardedPointsUseCase = revokeAwardedPointsUseCase;
    }

    @Override
    @Transactional
    public void execute(Long orderId, Long adminUserId) {
        Payment payment = loadPaymentPort.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for order: " + orderId));

        if (payment.status() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only COMPLETED payments can be refunded, current status: " + payment.status());
        }

        // Load order before cancellation to read loyaltyPointsAwarded
        Order order = loadOrderPort.loadById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        // Call the gateway
        PaymentPort.RefundResult result = paymentPort.refund(
                payment.providerTransactionId(), payment.amount());

        if (!result.success()) {
            throw new IllegalStateException("Refund failed: " + result.message());
        }

        // Persist REFUNDED state
        savePaymentPort.save(payment.refunded());

        // Cancel the order: restores stock and any redeemed loyalty points
        cancelOrderUseCase.execute(orderId, adminUserId, "Refunded by admin");

        // Revoke cashback points that were awarded when the payment was confirmed
        if (order.loyaltyPointsAwarded() > 0) {
            revokeAwardedPointsUseCase.execute(order.userId(), order.loyaltyPointsAwarded());
        }
    }
}
