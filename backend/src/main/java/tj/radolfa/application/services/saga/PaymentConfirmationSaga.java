package tj.radolfa.application.services.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.SaveSagaLogPort;
import tj.radolfa.application.services.saga.steps.AwardLoyaltyPointsStep;
import tj.radolfa.application.services.saga.steps.FinalizeCartStep;
import tj.radolfa.application.services.saga.steps.MarkOrderPaidStep;
import tj.radolfa.application.services.saga.steps.MarkPaymentCompletedStep;
import tj.radolfa.domain.model.PaymentSagaLog;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Component
public class PaymentConfirmationSaga {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationSaga.class);

    private final List<SagaStep<PaymentConfirmationContext>> steps;
    private final SaveSagaLogPort sagaLogPort;

    @Autowired
    public PaymentConfirmationSaga(MarkPaymentCompletedStep markPaymentCompleted,
                                    MarkOrderPaidStep markOrderPaid,
                                    AwardLoyaltyPointsStep awardLoyaltyPoints,
                                    FinalizeCartStep finalizeCart,
                                    SaveSagaLogPort sagaLogPort) {
        this(List.of(markPaymentCompleted, markOrderPaid, awardLoyaltyPoints, finalizeCart), sagaLogPort);
    }

    // Package-private test constructor — allows injecting arbitrary fake steps
    PaymentConfirmationSaga(List<SagaStep<PaymentConfirmationContext>> steps,
                             SaveSagaLogPort sagaLogPort) {
        this.steps       = steps;
        this.sagaLogPort = sagaLogPort;
    }

    public void execute(String providerTransactionId) {
        PaymentConfirmationContext ctx = new PaymentConfirmationContext(providerTransactionId);
        List<SagaStep<PaymentConfirmationContext>> completed = new ArrayList<>();

        for (SagaStep<PaymentConfirmationContext> step : steps) {
            try {
                step.execute(ctx);
                sagaLogPort.save(PaymentSagaLog.success(providerTransactionId, step.name()));
                completed.add(step);
            } catch (RuntimeException ex) {
                sagaLogPort.save(PaymentSagaLog.failed(providerTransactionId, step.name(), ex.getMessage()));
                log.error("[SAGA] Step {} failed: {}. Starting compensation.", step.name(), ex.getMessage());
                compensate(completed, ctx, providerTransactionId);
                throw new RuntimeException(
                        "Payment confirmation saga failed at step " + step.name(), ex);
            }
        }
    }

    private void compensate(List<SagaStep<PaymentConfirmationContext>> completed,
                             PaymentConfirmationContext ctx,
                             String providerTransactionId) {
        ListIterator<SagaStep<PaymentConfirmationContext>> it = completed.listIterator(completed.size());
        while (it.hasPrevious()) {
            SagaStep<PaymentConfirmationContext> step = it.previous();
            try {
                step.compensate(ctx);
                sagaLogPort.save(PaymentSagaLog.compensated(providerTransactionId, step.name(), null));
            } catch (RuntimeException ex) {
                sagaLogPort.save(PaymentSagaLog.compensated(providerTransactionId, step.name(), ex.getMessage()));
                log.error("[SAGA COMPENSATE] Compensation failed for {}: {}", step.name(), ex.getMessage());
                // best-effort: continue compensating remaining steps
            }
        }
    }
}
