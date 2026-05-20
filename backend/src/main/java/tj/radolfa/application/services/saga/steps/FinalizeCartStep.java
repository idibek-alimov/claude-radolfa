package tj.radolfa.application.services.saga.steps;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.application.services.saga.SagaStep;

@Component
public class FinalizeCartStep implements SagaStep<PaymentConfirmationContext> {

    private final LoadCartPort loadCartPort;
    private final SaveCartPort saveCartPort;

    public FinalizeCartStep(LoadCartPort loadCartPort, SaveCartPort saveCartPort) {
        this.loadCartPort = loadCartPort;
        this.saveCartPort = saveCartPort;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(PaymentConfirmationContext ctx) {
        loadCartPort.findByPendingOrderId(ctx.payment.orderId()).ifPresent(cart -> {
            cart.checkout();
            saveCartPort.save(cart);
            ctx.cartFinalized = true;
        });
    }

    @Override
    public void compensate(PaymentConfirmationContext ctx) {
        // No-op: the cart is a soft notion; the user can re-checkout if needed.
    }
}
