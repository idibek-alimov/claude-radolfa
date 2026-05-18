package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.order.GetPickpointSummariesUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadPickpointStatsPort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GetPickpointSummariesService implements GetPickpointSummariesUseCase {

    private final LoadPickpointStatsPort loadPickpointStatsPort;
    private final LoadPickpointPort      loadPickpointPort;

    public GetPickpointSummariesService(LoadPickpointStatsPort loadPickpointStatsPort,
                                        LoadPickpointPort loadPickpointPort) {
        this.loadPickpointStatsPort = loadPickpointStatsPort;
        this.loadPickpointPort      = loadPickpointPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickpointSummary> execute() {
        Instant overdueCutoff = Instant.now().minus(7, ChronoUnit.DAYS);

        Map<Long, LoadPickpointStatsPort.OrderCountRow> orderStats =
                loadPickpointStatsPort.countOrdersByPickpointAndStatus(overdueCutoff)
                        .stream()
                        .collect(Collectors.toMap(LoadPickpointStatsPort.OrderCountRow::pickpointId, r -> r));

        Map<Long, Long> returnStats =
                loadPickpointStatsPort.countCustomerReturnsReceived()
                        .stream()
                        .collect(Collectors.toMap(
                                LoadPickpointStatsPort.CustomerReturnCountRow::pickpointId,
                                LoadPickpointStatsPort.CustomerReturnCountRow::count));

        return loadPickpointPort.findAllActive().stream()
                .map(p -> {
                    LoadPickpointStatsPort.OrderCountRow row = orderStats.get(p.id());
                    long customerReturns = returnStats.getOrDefault(p.id(), 0L);
                    if (row == null) {
                        return new PickpointSummary(p.id(), p.name(), 0, 0, 0, 0, (int) customerReturns);
                    }
                    return new PickpointSummary(
                            p.id(), p.name(),
                            (int) row.incoming(),
                            (int) row.awaitingPickup(),
                            (int) row.overdue(),
                            (int) row.returnInProgress(),
                            (int) customerReturns);
                })
                .sorted(Comparator.comparing(PickpointSummary::name))
                .toList();
    }
}
