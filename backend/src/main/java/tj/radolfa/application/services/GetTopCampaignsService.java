package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.discount.GetTopCampaignsUseCase;
import tj.radolfa.application.ports.out.QueryDiscountMetricsPort;
import tj.radolfa.domain.model.TopCampaignRow;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class GetTopCampaignsService implements GetTopCampaignsUseCase {

    private static final Set<String> VALID_BY = Set.of("revenue", "units");
    private static final Set<String> VALID_PERIOD = Set.of("7d", "30d", "90d");

    private final QueryDiscountMetricsPort queryDiscountMetricsPort;

    public GetTopCampaignsService(QueryDiscountMetricsPort queryDiscountMetricsPort) {
        this.queryDiscountMetricsPort = queryDiscountMetricsPort;
    }

    @Override
    public List<TopCampaignRow> execute(Command command) {
        String by = (command.by() != null && VALID_BY.contains(command.by()))
                ? command.by() : "revenue";
        String period = (command.period() != null && VALID_PERIOD.contains(command.period()))
                ? command.period() : "30d";

        int days = switch (period) {
            case "7d"  -> 7;
            case "90d" -> 90;
            default    -> 30;
        };

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);

        return queryDiscountMetricsPort.findTop(by, from, to, command.limit() > 0 ? command.limit() : 5);
    }
}
