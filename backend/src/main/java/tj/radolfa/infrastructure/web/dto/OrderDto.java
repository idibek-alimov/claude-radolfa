package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        String status,
        BigDecimal totalAmount,
        List<OrderItemDto> items,
        Instant createdAt) {
}
