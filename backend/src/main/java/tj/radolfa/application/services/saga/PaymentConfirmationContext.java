package tj.radolfa.application.services.saga;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.Payment;

public class PaymentConfirmationContext {

    public final String providerTransactionId;
    public Payment payment;          // populated by MarkPaymentCompletedStep
    public Order   order;            // populated by MarkOrderPaidStep
    public boolean loyaltyAwarded;   // set by AwardLoyaltyPointsStep
    public boolean cartFinalized;    // set by FinalizeCartStep

    public PaymentConfirmationContext(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }
}
