package tj.radolfa.domain.model;

import java.time.Instant;
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
        int loyaltyPointsAwarded) {

    public Order {
        items = items == null ? List.of() : Collections.unmodifiableList(items);
    }
}
