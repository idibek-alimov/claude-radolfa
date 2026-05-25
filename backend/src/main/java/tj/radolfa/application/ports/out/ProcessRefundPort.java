package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.Money;

public interface ProcessRefundPort {

    record RefundResult(boolean success, String gatewayRefundId, String failureReason) {}

    RefundResult process(Long orderId, String externalPaymentId, Money amount);
}
