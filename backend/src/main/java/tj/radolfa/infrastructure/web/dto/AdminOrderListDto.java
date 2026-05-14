package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderListDto(
        Long id,
        String userPhone,
        String userName,
        Instant createdAt,
        String status,
        BigDecimal totalAmount,
        String deliveryType,
        int itemCount) {
}
