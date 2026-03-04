package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;

public record CartItemDto(
        Long skuId,
        String listingSlug,
        String productName,
        String sizeLabel,
        String imageUrl,
        BigDecimal priceSnapshot,
        int quantity,
        BigDecimal itemSubtotal) {
}
