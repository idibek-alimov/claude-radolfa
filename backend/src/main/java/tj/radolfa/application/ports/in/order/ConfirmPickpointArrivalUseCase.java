package tj.radolfa.application.ports.in.order;

public interface ConfirmPickpointArrivalUseCase {
    void execute(Long orderId, Long staffUserId);
}
