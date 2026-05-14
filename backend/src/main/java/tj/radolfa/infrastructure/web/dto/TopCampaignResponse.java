package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.TopCampaignRow;

import java.math.BigDecimal;

public record TopCampaignResponse(
        CampaignSummaryResponse campaign,
        long ordersUsing,
        long unitsMoved,
        BigDecimal revenueUplift
) {
    public static TopCampaignResponse fromDomain(TopCampaignRow row) {
        return new TopCampaignResponse(
                CampaignSummaryResponse.fromDomain(row.campaign()),
                row.ordersUsing(),
                row.unitsMoved(),
                row.revenueUplift()
        );
    }
}
