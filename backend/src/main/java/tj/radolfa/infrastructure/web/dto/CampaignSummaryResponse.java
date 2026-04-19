package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.AmountType;
import tj.radolfa.domain.model.DiscountSummary;

import java.math.BigDecimal;

public record CampaignSummaryResponse(
        Long id,
        String title,
        String colorHex,
        BigDecimal amountValue,
        AmountType amountType,
        DiscountTypeResponse type
) {
    public static CampaignSummaryResponse fromDomain(DiscountSummary s) {
        return new CampaignSummaryResponse(
                s.id(), s.title(), s.colorHex(), s.amountValue(), s.amountType(),
                DiscountTypeResponse.fromDomain(s.type())
        );
    }
}
