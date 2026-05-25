package tj.radolfa.application.services.saga.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadPaymentPort;
import tj.radolfa.application.ports.out.ProcessRefundPort;
import tj.radolfa.application.ports.out.SavePaymentPort;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.application.services.saga.SagaStep;
import tj.radolfa.domain.model.PaymentStatus;

@Component
public class MarkPaymentCompletedStep implements SagaStep<PaymentConfirmationContext> {

    private static final Logger log = LoggerFactory.getLogger(MarkPaymentCompletedStep.class);

    private final LoadPaymentPort   loadPaymentPort;
    private final SavePaymentPort   savePaymentPort;
    private final ProcessRefundPort processRefundPort;

    public MarkPaymentCompletedStep(LoadPaymentPort loadPaymentPort,
                                    SavePaymentPort savePaymentPort,
                                    ProcessRefundPort processRefundPort) {
        this.loadPaymentPort   = loadPaymentPort;
        this.savePaymentPort   = savePaymentPort;
        this.processRefundPort = processRefundPort;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(PaymentConfirmationContext ctx) {
        var payment = loadPaymentPort.findByProviderTransactionId(ctx.providerTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for transaction: " + ctx.providerTransactionId));

        if (payment.status() == PaymentStatus.COMPLETED) {
            ctx.payment = payment;
            return;
        }

        ctx.payment = savePaymentPort.save(payment.completed(ctx.providerTransactionId));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(PaymentConfirmationContext ctx) {
        if (ctx.payment == null || ctx.payment.status() != PaymentStatus.COMPLETED) {
            return;
        }
        savePaymentPort.save(ctx.payment.failed());
        var result = processRefundPort.process(
                ctx.payment.orderId(),
                ctx.payment.providerTransactionId(),
                ctx.payment.amount());
        if (result.success()) {
            log.error("[SAGA COMPENSATE] Refund issued for orderId={}. gatewayRefundId={}",
                    ctx.payment.orderId(), result.gatewayRefundId());
        } else {
            log.error("[SAGA COMPENSATE] Refund FAILED for orderId={}. reason={}",
                    ctx.payment.orderId(), result.failureReason());
        }
    }
}
