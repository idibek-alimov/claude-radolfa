package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.readmodel.SellerOrderItemRow;
import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * REST response DTO for {@code GET /api/v1/seller/me/orders}.
 * Read-only — sellers have no fulfilment controls; Radolfa fulfils everything.
 */
public record SellerOrderItemDto(
        Long orderItemId,
        Long orderId,
        OrderStatus orderStatus,
        Instant orderCreatedAt,
        String productName,
        String skuCode,
        int quantity,
        BigDecimal price
) {
    public static SellerOrderItemDto from(SellerOrderItemRow row) {
        return new SellerOrderItemDto(
                row.orderItemId(),
                row.orderId(),
                row.orderStatus(),
                row.orderCreatedAt(),
                row.productName(),
                row.skuCode(),
                row.quantity(),
                row.price());
    }
}
