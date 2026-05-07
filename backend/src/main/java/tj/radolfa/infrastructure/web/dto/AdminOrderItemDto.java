package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;

public record AdminOrderItemDto(
        String productName,
        int quantity,
        BigDecimal price,
        Long skuId,
        Long listingVariantId,
        String imageUrl,
        String skuCode,
        String sizeLabel,
        String slug,
        boolean hasReviewed,
        Integer currentStock) {}
