package tj.radolfa.application.ports.in.order;

public interface MarkOutForDeliveryUseCase {
    void execute(Long orderId, Long courierId);
}
