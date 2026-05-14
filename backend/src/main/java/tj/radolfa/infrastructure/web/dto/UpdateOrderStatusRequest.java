package tj.radolfa.infrastructure.web.dto;

import java.time.LocalDate;

public record UpdateOrderStatusRequest(
        String status,
        String courierName,
        String trackingNumber,
        LocalDate estimatedDeliveryDate) {}
