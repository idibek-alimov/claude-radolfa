package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetric(
        LocalDate date,
        long orders,
        long units,
        BigDecimal uplift
) {}
