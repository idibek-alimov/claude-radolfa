package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.List;

public record Order(
        Long id,
        Long userId,
        OrderStatus status,
        Money totalAmount,
        List<OrderItem> items,
        Instant createdAt) {
}
