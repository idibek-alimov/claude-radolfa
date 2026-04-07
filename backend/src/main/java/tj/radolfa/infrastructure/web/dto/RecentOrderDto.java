package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RecentOrderDto(
        Long orderId,
        String userPhone,
        BigDecimal totalAmount,
        String status,
        Instant createdAt
) {}
