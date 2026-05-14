package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record DiscountedProductRow(
        Long skuId,
        String skuCode,
        String sizeLabel,
        Integer stockQuantity,
        BigDecimal originalPrice,
        BigDecimal finalPrice,
        BigDecimal deltaPercent,
        DiscountSummary winningCampaign,
        List<DiscountSummary> otherCampaigns,
        Long productBaseId,
        String productName,
        Long variantId,
        String productCode,
        String imageUrl
) {}
