package tj.radolfa.application.ports.in.user;

import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;

public interface CreateCourierUserUseCase {
    record Command(String phone, String name, VehicleType vehicleType,
                   BigDecimal maxPayloadKg, Integer maxLengthCm,
                   Integer maxWidthCm, Integer maxHeightCm) {}
    Long execute(Command command);
}
