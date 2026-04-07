package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.AdminOrderSummary;
import tj.radolfa.application.ports.out.AdminOrderSummaryPort;
import tj.radolfa.infrastructure.persistence.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class AdminOrderSummaryAdapter implements AdminOrderSummaryPort {

    private final OrderRepository repository;

    public AdminOrderSummaryAdapter(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public AdminOrderSummary load() {
        ZonedDateTime nowUtc = Instant.now().atZone(ZoneOffset.UTC);
        Instant startOfToday = nowUtc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfMonth = nowUtc.toLocalDate().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long totalOrders       = repository.countAllOrders();
        long todayOrders       = repository.countOrdersSince(startOfToday);
        BigDecimal revenueToday  = repository.sumRevenueSince(startOfToday);
        BigDecimal revenueMonth  = repository.sumRevenueSince(startOfMonth);

        List<AdminOrderSummary.RecentOrder> recentOrders = repository
                .findMostRecent(PageRequest.of(0, 10))
                .stream()
                .map(o -> new AdminOrderSummary.RecentOrder(
                        o.getId(),
                        o.getUser().getPhone(),
                        o.getTotalAmount(),
                        o.getStatus().name(),
                        o.getCreatedAt()))
                .toList();

        return new AdminOrderSummary(totalOrders, todayOrders, revenueToday, revenueMonth, recentOrders);
    }
}
