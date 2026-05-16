package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.order.CourierFleetEntry;
import tj.radolfa.application.ports.in.order.GetCourierFleetSummaryUseCase;
import tj.radolfa.application.ports.out.LoadCourierOrderStatsPort;
import tj.radolfa.application.ports.out.LoadCourierOrderStatsPort.CourierOrderStats;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class GetCourierFleetSummaryService implements GetCourierFleetSummaryUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadCourierOrderStatsPort loadCourierOrderStatsPort;

    public GetCourierFleetSummaryService(LoadUserPort loadUserPort,
                                         LoadCourierOrderStatsPort loadCourierOrderStatsPort) {
        this.loadUserPort = loadUserPort;
        this.loadCourierOrderStatsPort = loadCourierOrderStatsPort;
    }

    @Override
    public List<CourierFleetEntry> execute() {
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        List<User> couriers = loadUserPort.findByRoleAndEnabledTrue(UserRole.COURIER);
        Map<Long, CourierOrderStats> stats = loadCourierOrderStatsPort.loadStats(todayStart);

        return couriers.stream()
                .map(c -> {
                    CourierOrderStats s = stats.getOrDefault(c.id(), CourierOrderStats.empty());
                    return new CourierFleetEntry(
                            c.id(), c.name(), c.vehicleType(), c.maxPayloadKg(),
                            s.deliveredToday(), s.inTransit(), s.attempted());
                })
                .sorted(Comparator.comparing(CourierFleetEntry::name,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
