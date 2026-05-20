package tj.radolfa.application.services.saga.steps;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.ExpireOrderUseCase;
import tj.radolfa.application.ports.in.order.UpdateOrderStatusUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.application.services.saga.SagaStep;
import tj.radolfa.domain.model.OrderStatus;

@Component
public class MarkOrderPaidStep implements SagaStep<PaymentConfirmationContext> {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final LoadOrderPort            loadOrderPort;
    private final ExpireOrderUseCase       expireOrderUseCase;

    public MarkOrderPaidStep(UpdateOrderStatusUseCase updateOrderStatusUseCase,
                              LoadOrderPort loadOrderPort,
                              ExpireOrderUseCase expireOrderUseCase) {
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.loadOrderPort            = loadOrderPort;
        this.expireOrderUseCase       = expireOrderUseCase;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(PaymentConfirmationContext ctx) {
        updateOrderStatusUseCase.execute(new UpdateOrderStatusUseCase.Command(
                ctx.payment.orderId(), OrderStatus.PAID, null, null, null));
        ctx.order = loadOrderPort.loadById(ctx.payment.orderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found after status update: " + ctx.payment.orderId()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(PaymentConfirmationContext ctx) {
        if (ctx.order == null) {
            return;
        }
        expireOrderUseCase.execute(ctx.order.id(), "Payment saga compensation");
    }
}
