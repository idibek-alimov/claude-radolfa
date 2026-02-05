package tj.radolfa.domain.model;

import java.math.BigDecimal;

public record OrderItem(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal priceAtPurchase) {
}
