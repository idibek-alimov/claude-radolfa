package tj.radolfa.domain.model;

import java.util.List;

public record DiscountOverlapRow(
        String skuCode,
        DiscountSummary winningCampaign,
        List<DiscountSummary> losingCampaigns
) {}
