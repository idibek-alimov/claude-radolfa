package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.DeliveryCode;

public interface RegenerateDeliveryCodeUseCase {
    DeliveryCode execute(Long orderId);
}
