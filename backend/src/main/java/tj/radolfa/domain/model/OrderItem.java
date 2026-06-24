package tj.radolfa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderItem {
        private final Long id;
        private final Long skuId;
        private final Long listingVariantId;
        private final String skuCode;
        private final String productName;
        private final int quantity;
        /** Final unit price actually charged to the customer (post-discount). */
        private final Money price;
        private final int quantityPicked;
        private final Instant pickedAt;
        private final Long pickedByUserId;
        /** Snapshot of the product owner at checkout time. {@code null} = Radolfa-owned. */
        private final Long sellerId;
        /** Pre-discount base unit price (the cart's add-to-cart snapshot). {@code null} for items predating this audit trail. */
        private final Money originalUnitPrice;
        /** Which pricing mechanism produced {@link #price}. {@code null} for items predating this audit trail. */
        private final WinningMechanism mechanism;
        /** Total effective discount percent off {@link #originalUnitPrice}. {@code null} for items predating this audit trail. */
        private final BigDecimal effectiveDiscountPercent;
        /** The user's loyalty tier discount percent in effect at purchase, regardless of winning mechanism. {@code null} for items predating this audit trail. */
        private final BigDecimal loyaltyTierPercent;

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
                        Long sellerId,
                        Money originalUnitPrice,
                        WinningMechanism mechanism,
                        BigDecimal effectiveDiscountPercent,
                        BigDecimal loyaltyTierPercent) {
                if (quantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
                }
                if (effectiveDiscountPercent != null && effectiveDiscountPercent.signum() < 0) {
                        throw new IllegalArgumentException("effectiveDiscountPercent must not be negative, got: " + effectiveDiscountPercent);
                }
                if (loyaltyTierPercent != null && loyaltyTierPercent.signum() < 0) {
                        throw new IllegalArgumentException("loyaltyTierPercent must not be negative, got: " + loyaltyTierPercent);
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
                this.originalUnitPrice = originalUnitPrice;
                this.mechanism = mechanism;
                this.effectiveDiscountPercent = effectiveDiscountPercent;
                this.loyaltyTierPercent = loyaltyTierPercent;
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
        public Money getOriginalUnitPrice() { return originalUnitPrice; }
        public WinningMechanism getMechanism() { return mechanism; }
        public BigDecimal getEffectiveDiscountPercent() { return effectiveDiscountPercent; }
        public BigDecimal getLoyaltyTierPercent() { return loyaltyTierPercent; }

        public boolean isFullyPicked() {
                return quantityPicked >= quantity;
        }
}
