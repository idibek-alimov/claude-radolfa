package tj.radolfa.application.ports.in.order;

public interface RedirectToPickpointUseCase {

    record Command(Long orderId, Long pickpointId) {}

    void execute(Command command);
}
