package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.payment.ConfirmPaymentUseCase;
import tj.radolfa.application.services.saga.PaymentConfirmationSaga;

@Service
public class ConfirmPaymentService implements ConfirmPaymentUseCase {

    private final PaymentConfirmationSaga saga;

    public ConfirmPaymentService(PaymentConfirmationSaga saga) {
        this.saga = saga;
    }

    @Override
    public void execute(String providerTransactionId) {
        saga.execute(providerTransactionId);
    }
}
