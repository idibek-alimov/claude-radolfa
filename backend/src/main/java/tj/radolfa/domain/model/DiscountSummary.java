package tj.radolfa.domain.model;

import java.math.BigDecimal;

public record DiscountSummary(
        Long id,
        String title,
        String colorHex,
        BigDecimal discountValue,
        DiscountType type
) {}
