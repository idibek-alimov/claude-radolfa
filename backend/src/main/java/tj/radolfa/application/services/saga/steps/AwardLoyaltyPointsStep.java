package tj.radolfa.application.services.saga.steps;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AwardLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.loyalty.RevokeAwardedPointsUseCase;
import tj.radolfa.application.ports.out.LoadOrderPort;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.application.services.saga.SagaStep;
import tj.radolfa.domain.model.Order;

@Component
public class AwardLoyaltyPointsStep implements SagaStep<PaymentConfirmationContext> {

    private final AwardLoyaltyPointsUseCase  awardLoyaltyPointsUseCase;
    private final RevokeAwardedPointsUseCase revokeAwardedPointsUseCase;
    private final LoadOrderPort              loadOrderPort;

    public AwardLoyaltyPointsStep(AwardLoyaltyPointsUseCase awardLoyaltyPointsUseCase,
                                   RevokeAwardedPointsUseCase revokeAwardedPointsUseCase,
                                   LoadOrderPort loadOrderPort) {
        this.awardLoyaltyPointsUseCase  = awardLoyaltyPointsUseCase;
        this.revokeAwardedPointsUseCase = revokeAwardedPointsUseCase;
        this.loadOrderPort              = loadOrderPort;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(PaymentConfirmationContext ctx) {
        awardLoyaltyPointsUseCase.execute(ctx.order.userId(), ctx.order.id());
        // Reload the order to read the awarded count persisted by the award service
        Order reloaded = loadOrderPort.loadById(ctx.order.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found after loyalty award: " + ctx.order.id()));
        ctx.awardedPoints = reloaded.loyaltyPointsAwarded();
        ctx.loyaltyAwarded = true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(PaymentConfirmationContext ctx) {
        if (ctx.awardedPoints > 0) {
            revokeAwardedPointsUseCase.execute(ctx.order.userId(), ctx.awardedPoints);
        }
    }
}
