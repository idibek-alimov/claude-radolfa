package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.DailyMetric;
import tj.radolfa.domain.model.DiscountMetrics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DiscountMetricsResponse(
        long ordersUsing,
        long unitsMoved,
        BigDecimal revenueUplift,
        BigDecimal avgDiscountPerOrder,
        LocalDate from,
        LocalDate to,
        List<Daily> dailySeries
) {
    public record Daily(LocalDate date, long orders, long units, BigDecimal uplift) {
        public static Daily fromDomain(DailyMetric m) {
            return new Daily(m.date(), m.orders(), m.units(), m.uplift());
        }
    }

    public static DiscountMetricsResponse fromDomain(DiscountMetrics m) {
        return new DiscountMetricsResponse(
                m.ordersUsing(), m.unitsMoved(), m.revenueUplift(), m.avgDiscountPerOrder(),
                m.from(), m.to(),
                m.dailySeries().stream().map(Daily::fromDomain).toList()
        );
    }
}
