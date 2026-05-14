package tj.radolfa.domain.model;

import java.math.BigDecimal;

public record TopCampaignRow(
        DiscountSummary campaign,
        long ordersUsing,
        long unitsMoved,
        BigDecimal revenueUplift
) {}
