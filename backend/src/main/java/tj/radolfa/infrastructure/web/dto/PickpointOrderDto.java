package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** View of an order for the pickpoint staff dashboard. */
public record PickpointOrderDto(
        Long orderId,
        String customerFirstName,
        String customerPhone,
        OrderStatus status,
        Instant readyAt,
        Instant expiresAt,
        int daysUntilExpiry,
        boolean overdue,
        int daysOverdue) {

    public static PickpointOrderDto from(Order order, User customer, int storageDays) {
        String firstName = extractFirstName(customer != null ? customer.name() : null);
        String phone     = customer != null && customer.phone() != null ? customer.phone().value() : null;
        Instant readyAt  = order.readyForPickupAt() != null ? order.readyForPickupAt() : order.createdAt();
        Instant expiresAt = readyAt.plus(storageDays, ChronoUnit.DAYS);
        Instant now = Instant.now();

        boolean isOverdue = order.readyForPickupAt() != null && now.isAfter(expiresAt);
        int daysOverdueVal = isOverdue
                ? (int) ChronoUnit.DAYS.between(expiresAt, now)
                : 0;
        long hours = ChronoUnit.HOURS.between(now, expiresAt);
        int daysLeft = isOverdue ? 0 : (int) Math.max(0, (hours + 23) / 24);

        return new PickpointOrderDto(
                order.id(),
                firstName,
                phone,
                order.status(),
                readyAt,
                expiresAt,
                daysLeft,
                isOverdue,
                daysOverdueVal);
    }

    private static String extractFirstName(String name) {
        if (name == null || name.isBlank()) return null;
        String[] parts = name.strip().split("\\s+", 2);
        return parts[0];
    }
}
