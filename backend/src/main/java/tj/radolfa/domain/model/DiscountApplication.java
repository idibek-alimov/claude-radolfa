package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record DiscountApplication(
        Long id,
        Long discountId,
        Long orderId,
        Long orderLineId,
        String skuItemCode,
        int quantity,
        BigDecimal originalUnitPrice,
        BigDecimal appliedUnitPrice,
        BigDecimal discountAmount,
        Instant appliedAt
) {
    public DiscountApplication {
        Objects.requireNonNull(discountId, "discountId");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(orderLineId, "orderLineId");
        Objects.requireNonNull(skuItemCode, "skuItemCode");
        Objects.requireNonNull(originalUnitPrice, "originalUnitPrice");
        Objects.requireNonNull(appliedUnitPrice, "appliedUnitPrice");
        Objects.requireNonNull(discountAmount, "discountAmount");
        Objects.requireNonNull(appliedAt, "appliedAt");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
    }
}
