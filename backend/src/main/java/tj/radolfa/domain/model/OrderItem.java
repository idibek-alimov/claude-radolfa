package tj.radolfa.domain.model;

import java.time.Instant;

public class OrderItem {
        private final Long id;
        private final Long skuId;
        private final Long listingVariantId;
        private final String skuCode;
        private final String productName;
        private final int quantity;
        private final Money price;
        private final int quantityPicked;
        private final Instant pickedAt;
        private final Long pickedByUserId;
        /** Snapshot of the product owner at checkout time. {@code null} = Radolfa-owned. */
        private final Long sellerId;

        public OrderItem(Long id,
                        Long skuId,
                        Long listingVariantId,
                        String skuCode,
                        String productName,
                        int quantity,
                        Money price,
                        int quantityPicked,
                        Instant pickedAt,
                        Long pickedByUserId,
                        Long sellerId) {
                if (quantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
                }
                this.id = id;
                this.skuId = skuId;
                this.listingVariantId = listingVariantId;
                this.skuCode = skuCode;
                this.productName = productName;
                this.quantity = quantity;
                this.price = price;
                this.quantityPicked = quantityPicked;
                this.pickedAt = pickedAt;
                this.pickedByUserId = pickedByUserId;
                this.sellerId = sellerId;
        }

        public Long getId() { return id; }
        public Long getSkuId() { return skuId; }
        public Long getListingVariantId() { return listingVariantId; }
        public String getSkuCode() { return skuCode; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public Money getPrice() { return price; }
        public int getQuantityPicked() { return quantityPicked; }
        public Instant getPickedAt() { return pickedAt; }
        public Long getPickedByUserId() { return pickedByUserId; }
        public Long getSellerId() { return sellerId; }

        public boolean isFullyPicked() {
                return quantityPicked >= quantity;
        }
}
