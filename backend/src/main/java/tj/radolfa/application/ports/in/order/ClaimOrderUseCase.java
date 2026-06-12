package tj.radolfa.application.ports.in.order;

public interface ClaimOrderUseCase {

    record Command(Long orderId, Long courierId) {}

    void execute(Command cmd);
}
