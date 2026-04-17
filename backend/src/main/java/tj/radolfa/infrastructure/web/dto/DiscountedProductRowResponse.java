package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.domain.model.DiscountedProductRow;

import java.math.BigDecimal;
import java.util.List;

public record DiscountedProductRowResponse(
        Long skuId,
        String skuCode,
        String sizeLabel,
        Integer stockQuantity,
        BigDecimal originalPrice,
        BigDecimal finalPrice,
        BigDecimal deltaPercent,
        CampaignSummaryResponse winningCampaign,
        List<CampaignSummaryResponse> otherCampaigns,
        Long productBaseId,
        String productName,
        Long variantId,
        String productCode,
        String imageUrl
) {
    public static DiscountedProductRowResponse fromDomain(DiscountedProductRow row) {
        return new DiscountedProductRowResponse(
                row.skuId(), row.skuCode(), row.sizeLabel(), row.stockQuantity(),
                row.originalPrice(), row.finalPrice(), row.deltaPercent(),
                CampaignSummaryResponse.fromDomain(row.winningCampaign()),
                row.otherCampaigns().stream().map(CampaignSummaryResponse::fromDomain).toList(),
                row.productBaseId(), row.productName(), row.variantId(),
                row.productCode(), row.imageUrl()
        );
    }
}
