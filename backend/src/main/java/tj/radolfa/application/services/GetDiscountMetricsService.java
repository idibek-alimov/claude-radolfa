package tj.radolfa.application.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.discount.GetDiscountMetricsUseCase;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.QueryDiscountMetricsPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.domain.model.DiscountMetrics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class GetDiscountMetricsService implements GetDiscountMetricsUseCase {

    private final LoadDiscountPort loadDiscountPort;
    private final QueryDiscountMetricsPort queryDiscountMetricsPort;
    private final LocalDate analyticsStart;

    public GetDiscountMetricsService(LoadDiscountPort loadDiscountPort,
                                     QueryDiscountMetricsPort queryDiscountMetricsPort,
                                     @Value("${radolfa.analytics.start-date}") LocalDate analyticsStart) {
        this.loadDiscountPort = loadDiscountPort;
        this.queryDiscountMetricsPort = queryDiscountMetricsPort;
        this.analyticsStart = analyticsStart;
    }

    @Override
    public DiscountMetrics execute(Long discountId) {
        Discount campaign = loadDiscountPort.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + discountId));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate campaignEnd = campaign.validUpto().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate windowEnd = campaignEnd.isBefore(today) ? campaignEnd : today;

        // Campaign predates analytics — no data will ever exist
        if (windowEnd.isBefore(analyticsStart)) {
            return new DiscountMetrics(0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO,
                    analyticsStart, analyticsStart, List.of());
        }

        LocalDate campaignStart = campaign.validFrom().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate windowStart = campaignStart.isAfter(analyticsStart) ? campaignStart : analyticsStart;

        return queryDiscountMetricsPort.findMetrics(discountId, windowStart, windowEnd);
    }
}
