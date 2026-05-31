package tj.radolfa.application.readmodel;

import tj.radolfa.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model returned by {@link tj.radolfa.application.ports.out.LoadSellerOrderItemsPort}.
 * Joins the parent order with the individual order item so a seller dashboard can show
 * per-item order context without exposing the full Order aggregate.
 */
public record SellerOrderItemRow(
        Long orderItemId,
        Long orderId,
        OrderStatus orderStatus,
        Instant orderCreatedAt,
        String productName,
        String skuCode,
        int quantity,
        BigDecimal price
) {}
