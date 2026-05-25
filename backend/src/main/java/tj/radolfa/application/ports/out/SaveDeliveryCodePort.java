package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.DeliveryCode;

public interface SaveDeliveryCodePort {
    DeliveryCode save(DeliveryCode code);
    void invalidateAllForOrder(Long orderId);
}
