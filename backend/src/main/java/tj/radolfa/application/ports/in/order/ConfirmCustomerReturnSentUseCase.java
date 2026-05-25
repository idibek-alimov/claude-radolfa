package tj.radolfa.application.ports.in.order;

public interface ConfirmCustomerReturnSentUseCase {
    void execute(Long returnId, Long staffUserId);
}
