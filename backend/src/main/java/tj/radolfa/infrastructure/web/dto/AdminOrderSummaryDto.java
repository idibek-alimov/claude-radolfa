package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminOrderSummaryDto(
        long totalOrders,
        long todayOrders,
        BigDecimal revenueToday,
        BigDecimal revenueThisMonth,
        List<RecentOrderDto> recentOrders
) {}
