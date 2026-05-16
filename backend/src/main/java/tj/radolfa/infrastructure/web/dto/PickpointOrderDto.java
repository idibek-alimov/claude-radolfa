package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * View of an order for the pickpoint staff dashboard.
 * readyAt approximates from order.createdAt until a dedicated readyForPickupAt
 * timestamp is added in Phase 6.
 */
public record PickpointOrderDto(
        Long orderId,
        String customerFirstName,
        OrderStatus status,
        Instant readyAt,
        Instant expiresAt,
        int daysUntilExpiry) {

    public static PickpointOrderDto from(Order order, User customer, int storageDays) {
        String firstName = extractFirstName(customer.name());
        Instant readyAt  = order.createdAt();
        Instant expiresAt = readyAt.plus(storageDays, ChronoUnit.DAYS);
        long hours = ChronoUnit.HOURS.between(Instant.now(), expiresAt);
        int daysLeft = (int) Math.max(0, (hours + 23) / 24);

        return new PickpointOrderDto(
                order.id(),
                firstName,
                order.status(),
                readyAt,
                expiresAt,
                daysLeft);
    }

    private static String extractFirstName(String name) {
        if (name == null || name.isBlank()) return null;
        String[] parts = name.strip().split("\\s+", 2);
        return parts[0];
    }
}
