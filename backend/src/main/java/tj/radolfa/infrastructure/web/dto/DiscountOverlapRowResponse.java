package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.DiscountOverlapRow;

import java.util.List;

public record DiscountOverlapRowResponse(
        String skuCode,
        CampaignSummaryResponse winningCampaign,
        List<CampaignSummaryResponse> losingCampaigns
) {
    public static DiscountOverlapRowResponse fromDomain(DiscountOverlapRow row) {
        return new DiscountOverlapRowResponse(
                row.skuCode(),
                CampaignSummaryResponse.fromDomain(row.winningCampaign()),
                row.losingCampaigns().stream().map(CampaignSummaryResponse::fromDomain).toList()
        );
    }
}
