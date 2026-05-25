package tj.radolfa.application.ports.in.user;

import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;

public interface UpdateCourierDetailsUseCase {
    record Command(Long userId, VehicleType vehicleType, BigDecimal maxPayloadKg,
                   Integer maxLengthCm, Integer maxWidthCm, Integer maxHeightCm) {}
    void execute(Command command);
}
