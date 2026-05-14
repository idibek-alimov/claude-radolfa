package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OrderDto(
        Long id,
        String status,
        BigDecimal totalAmount,
        List<OrderItemDto> items,
        Instant createdAt,
        int loyaltyPointsRedeemed,
        int loyaltyPointsAwarded,
        String deliveryType,
        String deliveryAddress,
        String preferredTimeWindow,
        Long pickpointId,
        String pickpointName,
        String pickpointAddress,
        String courierName,
        String trackingNumber,
        LocalDate estimatedDeliveryDate) {
}
