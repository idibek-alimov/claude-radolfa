package tj.radolfa.application.ports.in.order;

public interface RetryDeliveryUseCase {
    void execute(Long orderId, Long courierId);
}
