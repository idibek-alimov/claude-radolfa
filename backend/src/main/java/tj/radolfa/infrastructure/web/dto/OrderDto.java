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
        LocalDate estimatedDeliveryDate,
        // Per-step timestamps for the order stepper (Profile redesign Phase 4)
        Instant claimedAt,
        Instant shippedAt,
        Instant outForDeliveryAt,
        Instant deliveryAttemptedAt,
        Instant readyForPickupAt,
        Instant deliveredAt,
        // Exception-state timestamps for the status banner
        Instant cancelledAt,
        Instant refundedAt,
        Instant returnInitiatedAt,
        Instant returnedToWarehouseAt,
        Instant recallRequestedAt,
        // Active delivery/pickup code — only populated for in-progress states that carry one
        String deliveryCode) {
}
