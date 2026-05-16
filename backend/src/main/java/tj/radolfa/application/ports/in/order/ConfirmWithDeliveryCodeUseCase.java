package tj.radolfa.application.ports.in.order;

public interface ConfirmWithDeliveryCodeUseCase {
    record Command(Long orderId, String enteredCode) {}
    void execute(Command command);
}
