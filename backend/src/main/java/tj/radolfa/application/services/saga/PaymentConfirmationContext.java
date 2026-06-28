package tj.radolfa.application.services.saga;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.Payment;

public class PaymentConfirmationContext {

    public final String providerTransactionId;
    public Payment payment;          // populated by MarkPaymentCompletedStep
    public Order   order;            // populated by MarkOrderPaidStep
    public boolean loyaltyAwarded;   // set by AwardLoyaltyPointsStep
    public int     awardedPoints;    // cashback credited by AwardLoyaltyPointsStep; used by compensate()
    public boolean cartFinalized;    // set by FinalizeCartStep
    public Long    finalizedCartId;  // cart id captured by FinalizeCartStep.execute() for compensation
                                     // (pendingOrderId is nulled by checkout(), so id is the only
                                     //  reliable handle after the step commits)

    public PaymentConfirmationContext(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }
}
