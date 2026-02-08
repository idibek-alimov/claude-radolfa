package tj.radolfa.domain.model;

public record OrderItem(
        Long id,
        Long productId,
        String productName,
        int quantity,
        Money priceAtPurchase) {
}
