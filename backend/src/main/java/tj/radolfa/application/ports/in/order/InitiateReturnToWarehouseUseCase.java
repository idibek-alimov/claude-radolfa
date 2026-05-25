package tj.radolfa.application.ports.in.order;

public interface InitiateReturnToWarehouseUseCase {
    void execute(Long orderId, Long initiatingUserId);
}
