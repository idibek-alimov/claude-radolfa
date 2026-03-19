package tj.radolfa.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AwardLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.in.payment.ConfirmPaymentUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.domain.model.PaymentStatus;

/**
 * Handles a successful payment callback from the provider.
 *
 * <ol>
 *   <li>Find the payment by provider transaction ID.</li>
 *   <li>Idempotency guard: skip if already COMPLETED.</li>
 *   <li>Transition the {@code Payment} to COMPLETED and persist.</li>
 *   <li>Transition the linked {@code Order} to PAID.</li>
 *   <li>Trigger loyalty points award (Phase 9 provides the real implementation).</li>
 * </ol>
 */
@Service
public class ConfirmPaymentService implements ConfirmPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmPaymentService.class);

    private final LoadPaymentPort          loadPaymentPort;
    private final SavePaymentPort          savePaymentPort;
    private final LoadOrderPort            loadOrderPort;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final AwardLoyaltyPointsUseCase awardLoyaltyPointsUseCase;

    public ConfirmPaymentService(LoadPaymentPort loadPaymentPort,
                                 SavePaymentPort savePaymentPort,
                                 LoadOrderPort loadOrderPort,
                                 UpdateOrderStatusUseCase updateOrderStatusUseCase,
                                 AwardLoyaltyPointsUseCase awardLoyaltyPointsUseCase) {
        this.loadPaymentPort          = loadPaymentPort;
        this.savePaymentPort          = savePaymentPort;
        this.loadOrderPort            = loadOrderPort;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.awardLoyaltyPointsUseCase = awardLoyaltyPointsUseCase;
    }

    @Override
    @Transactional
    public void execute(String providerTransactionId, String webhookPayload) {
        Payment payment = loadPaymentPort.findByProviderTransactionId(providerTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for transaction: " + providerTransactionId));

        // Idempotency: skip if already processed
        if (payment.status() == PaymentStatus.COMPLETED) {
            log.info("[ConfirmPayment] Already COMPLETED — skipping. tx={}", providerTransactionId);
            return;
        }

        // Transition payment to COMPLETED
        Payment completed = payment.completed(providerTransactionId);
        savePaymentPort.save(completed);

        // Transition order to PAID
        updateOrderStatusUseCase.execute(payment.orderId(), OrderStatus.PAID);

        // Award loyalty points (Phase 9 provides real implementation)
        Long userId = loadOrderPort.loadById(payment.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + payment.orderId()))
                .userId();
        awardLoyaltyPointsUseCase.execute(userId, payment.orderId());
    }
}
