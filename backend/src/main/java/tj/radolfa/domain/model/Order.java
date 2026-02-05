package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItem> items,
        Instant createdAt) {
}
