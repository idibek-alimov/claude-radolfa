package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.domain.model.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * View of an order for the courier mobile dashboard.
 * totalWeightKg is null if any SKU's weight is missing.
 */
public record CourierOrderDto(
        Long orderId,
        String customerFirstName,
        String customerPhone,
        String deliveryAddress,
        String preferredTimeWindow,
        OrderStatus status,
        int deliveryAttemptCount,
        int totalItemCount,
        BigDecimal totalWeightKg,
        Instant shippedAt,
        Instant outForDeliveryAt) {

    public static CourierOrderDto from(Order order, User customer, Map<Long, Sku> skuMap) {
        String firstName = extractFirstName(customer.name());
        int itemCount = order.items() == null ? 0
                : order.items().stream().mapToInt(OrderItem::getQuantity).sum();
        BigDecimal totalWeight = computeTotalWeight(order, skuMap);

        return new CourierOrderDto(
                order.id(),
                firstName,
                customer.phone().value(),
                order.deliveryAddress(),
                order.preferredTimeWindow(),
                order.status(),
                order.deliveryAttemptCount(),
                itemCount,
                totalWeight,
                order.shippedAt(),
                order.outForDeliveryAt());
    }

    private static String extractFirstName(String name) {
        if (name == null || name.isBlank()) return null;
        String[] parts = name.strip().split("\\s+", 2);
        return parts[0];
    }

    private static BigDecimal computeTotalWeight(Order order, Map<Long, Sku> skuMap) {
        if (order.items() == null || order.items().isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.items()) {
            if (item.getSkuId() == null) return null;
            Sku sku = skuMap.get(item.getSkuId());
            if (sku == null || sku.getWeightKg() == null) return null;
            total = total.add(sku.getWeightKg().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }
}
