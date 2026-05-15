package tj.radolfa.infrastructure.web.dto;

import java.time.LocalDate;

public record UpdateOrderStatusRequest(
        String status,
        Long courierId,
        String trackingNumber,
        LocalDate estimatedDeliveryDate) {}
