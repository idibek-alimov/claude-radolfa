package tj.radolfa.application.ports.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read-side aggregate returned by {@link AdminOrderSummaryPort}.
 * Lives in the application layer (not domain — no business behaviour;
 * not infrastructure — no JPA/Jackson annotations).
 */
public record AdminOrderSummary(
        long totalOrders,
        long todayOrders,
        BigDecimal revenueToday,
        BigDecimal revenueThisMonth,
        List<RecentOrder> recentOrders
) {
    public record RecentOrder(
            Long orderId,
            String userPhone,
            BigDecimal totalAmount,
            String status,
            Instant createdAt
    ) {}
}
