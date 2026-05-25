package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.DeliveryAttemptReason;

public interface MarkDeliveryAttemptedUseCase {
    record Command(Long orderId, Long courierId, DeliveryAttemptReason reason, String photoUrl) {}
    void execute(Command command);
}
