package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.DeliveryCode;

import java.util.Optional;

public interface LoadDeliveryCodeByValuePort {
    Optional<DeliveryCode> loadActiveByCode(String code);
}
