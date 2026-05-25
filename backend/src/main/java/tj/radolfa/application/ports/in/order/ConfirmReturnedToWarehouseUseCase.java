package tj.radolfa.application.ports.in.order;

public interface ConfirmReturnedToWarehouseUseCase {
    void execute(Long orderId, Long staffUserId);
}
