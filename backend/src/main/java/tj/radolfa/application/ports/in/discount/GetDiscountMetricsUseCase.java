package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.DiscountMetrics;

public interface GetDiscountMetricsUseCase {

    DiscountMetrics execute(Long discountId);
}
