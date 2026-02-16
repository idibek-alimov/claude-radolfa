package tj.radolfa.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public record Order(
        Long id,
        Long userId,
        String erpOrderId,
        OrderStatus status,
        Money totalAmount,
        List<OrderItem> items,
        Instant createdAt) {

    public Order {
        items = items == null ? List.of() : Collections.unmodifiableList(items);
    }
}
