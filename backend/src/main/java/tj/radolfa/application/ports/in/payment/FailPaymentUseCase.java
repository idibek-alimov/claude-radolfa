package tj.radolfa.application.ports.in.payment;

public interface FailPaymentUseCase {
    void execute(String providerTransactionId);
}
