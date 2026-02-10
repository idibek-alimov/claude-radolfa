package tj.radolfa.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Purchasable unit — one size of one colour variant.
 * Displayed on the product detail page as a size selector.
 */
public record SkuDto(
        Long id,
        String erpItemCode,
        String sizeLabel,
        Integer stockQuantity,
        BigDecimal price,
        BigDecimal salePrice,
        boolean onSale,
        Instant saleEndsAt
) {}
