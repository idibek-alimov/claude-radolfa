package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.order.CourierFleetEntry;
import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;

public record CourierFleetDto(
        Long courierId,
        String name,
        VehicleType vehicleType,
        BigDecimal maxPayloadKg,
        long deliveredToday,
        long inTransit,
        long attempted) {

    public static CourierFleetDto from(CourierFleetEntry entry) {
        return new CourierFleetDto(
                entry.courierId(),
                entry.name(),
                entry.vehicleType(),
                entry.maxPayloadKg(),
                entry.deliveredToday(),
                entry.inTransit(),
                entry.attempted());
    }
}
