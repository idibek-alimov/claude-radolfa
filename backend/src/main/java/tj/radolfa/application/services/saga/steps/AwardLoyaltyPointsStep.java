package tj.radolfa.application.services.saga.steps;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.loyalty.AwardLoyaltyPointsUseCase;
import tj.radolfa.application.services.saga.PaymentConfirmationContext;
import tj.radolfa.application.services.saga.SagaStep;

@Component
public class AwardLoyaltyPointsStep implements SagaStep<PaymentConfirmationContext> {

    private final AwardLoyaltyPointsUseCase awardLoyaltyPointsUseCase;

    public AwardLoyaltyPointsStep(AwardLoyaltyPointsUseCase awardLoyaltyPointsUseCase) {
        this.awardLoyaltyPointsUseCase = awardLoyaltyPointsUseCase;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(PaymentConfirmationContext ctx) {
        awardLoyaltyPointsUseCase.execute(ctx.order.userId(), ctx.order.id());
        ctx.loyaltyAwarded = true;
    }

    @Override
    public void compensate(PaymentConfirmationContext ctx) {
        // No-op: ExpireOrderUseCase (step 2 compensator) restores redeemed loyalty
        // points. Reversing earned points is out of scope.
    }
}
