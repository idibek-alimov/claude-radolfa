package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DiscountMetrics(
        long ordersUsing,
        long unitsMoved,
        BigDecimal revenueUplift,
        BigDecimal avgDiscountPerOrder,
        LocalDate from,
        LocalDate to,
        List<DailyMetric> dailySeries
) {}
