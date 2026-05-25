package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;

public record CourierSummaryDto(
        Long id,
        String name,
        String phone,
        VehicleType vehicleType,
        BigDecimal maxPayloadKg) {

    public static CourierSummaryDto from(User user) {
        return new CourierSummaryDto(
                user.id(),
                user.name(),
                user.phone().value(),
                user.vehicleType(),
                user.maxPayloadKg());
    }
}
