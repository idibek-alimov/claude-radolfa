package tj.radolfa.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record Order(
        Long id,
        Long userId,
        String externalOrderId,
        OrderStatus status,
        Money totalAmount,
        List<OrderItem> items,
        Instant createdAt,
        int loyaltyPointsRedeemed,
        int loyaltyPointsAwarded,
        DeliveryType deliveryType,
        String deliveryAddress,
        String preferredTimeWindow,
        Long pickpointId,
        String courierName,
        String trackingNumber,
        LocalDate estimatedDeliveryDate,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt) {

    public Order {
        items = items == null ? List.of() : Collections.unmodifiableList(items);
    }
}
