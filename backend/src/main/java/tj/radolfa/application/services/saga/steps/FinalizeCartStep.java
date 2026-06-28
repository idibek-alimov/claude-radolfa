package tj.radolfa.application.services.saga.steps;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.out.LoadCartPort;
import tj.radolfa.application.ports.out.SaveCartPort;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.application.services.saga.SagaStep;
import tj.radolfa.domain.model.Cart;

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
            Cart saved = saveCartPort.save(cart);
            ctx.finalizedCartId = saved.getId();
            ctx.cartFinalized   = true;
        });
    }

    /**
     * Restores a checked-out cart to ACTIVE so the user can retry checkout.
     *
     * <p>Note: {@code cart.checkout()} nulls {@code pendingOrderId}, so after this step
     * commits, neither {@code findByPendingOrderId} nor {@code findActiveByUserId} can locate
     * the cart. We capture the id in {@link PaymentConfirmationContext#finalizedCartId} during
     * {@link #execute} and reload via {@code findById} here.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(PaymentConfirmationContext ctx) {
        if (ctx.cartFinalized && ctx.finalizedCartId != null) {
            loadCartPort.findById(ctx.finalizedCartId).ifPresent(cart -> {
                cart.reopen();
                saveCartPort.save(cart);
            });
        }
    }
}
