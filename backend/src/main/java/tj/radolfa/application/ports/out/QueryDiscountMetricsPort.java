package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.DiscountMetrics;
import tj.radolfa.domain.model.TopCampaignRow;

import java.time.LocalDate;
import java.util.List;

public interface QueryDiscountMetricsPort {

    DiscountMetrics findMetrics(Long discountId, LocalDate from, LocalDate to);

    List<TopCampaignRow> findTop(String by, LocalDate from, LocalDate to, int limit);
}
