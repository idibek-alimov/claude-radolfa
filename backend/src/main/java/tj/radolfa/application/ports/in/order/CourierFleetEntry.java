package tj.radolfa.application.ports.in.order;

import tj.radolfa.domain.model.VehicleType;

import java.math.BigDecimal;

public record CourierFleetEntry(
        Long courierId,
        String name,
        VehicleType vehicleType,
        BigDecimal maxPayloadKg,
        long deliveredToday,
        long inTransit,
        long attempted) {}
